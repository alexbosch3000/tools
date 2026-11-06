// Public API of the shopping module: lists, their items and recipes, and completion of a shopping trip.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.CatalogService;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.recipes.RecipeIngredient;
import io.github.alexbosch3000.familyhub.recipes.RecipeService;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingLine;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRepository;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRepository.ListRow;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRows;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingService {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingService.class);
    private static final int DONE_LISTS_SHOWN = 20;

    private final ShoppingRepository repository;
    private final CatalogService catalog;
    private final RecipeService recipes;
    private final MemberService members;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ShoppingService(ShoppingRepository repository, CatalogService catalog, RecipeService recipes,
            MemberService members, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.recipes = recipes;
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    /** Open lists, or the most recent completed ones. */
    public List<ShoppingListSummary> list(ShoppingStatus status) {
        return repository.findListIds(status, DONE_LISTS_SHOWN).stream()
                .map(this::get).map(ShoppingListSummary::of).toList();
    }

    public ShoppingList get(long id) {
        ListRow list = findList(id);
        List<ShoppingLine> lines = repository.findLines(id);
        List<ListedRecipe> listed = repository.findRecipes(id);
        Map<Long, Item> items = catalog.getAll(lines.stream().map(ShoppingLine::itemId).distinct().toList());
        Map<Long, String> recipeNames = listed.stream()
                .collect(Collectors.toMap(ListedRecipe::entryId, ListedRecipe::name));
        List<ShoppingRow> rows = ShoppingRows.build(lines, items, recipeNames, repository.findPurchases(id));
        long estimated = rows.stream().map(ShoppingRow::estimatedCents).filter(Objects::nonNull)
                .mapToLong(Long::longValue).sum();
        int unpriced = (int) rows.stream().filter(r -> r.estimatedCents() == null).count();
        return new ShoppingList(list.id(), list.name(), list.status(), list.plannedFor(), list.completedOn(),
                list.amountPaidCents(), rows, listed, estimated, unpriced);
    }

    @Transactional
    public ShoppingList create(String name, @Nullable LocalDate plannedFor) {
        return get(repository.insertList(name, plannedFor, LocalDateTime.now(clock)));
    }

    @Transactional
    public ShoppingList update(long id, String name, @Nullable LocalDate plannedFor) {
        findList(id);
        repository.updateList(id, name, plannedFor);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        findList(id);
        repository.deleteList(id);
    }

    /** Adds an item, for a given member (clothes, shoes) or for the household when memberId is null. */
    @Transactional
    public ShoppingList addItem(long listId, long itemId, @Nullable Long memberId, Quantity quantity,
            @Nullable String note) {
        requireOpen(listId);
        catalog.get(itemId);
        if (memberId != null && !members.exists(memberId)) {
            throw new InvalidRequestException("Membre inconnu.");
        }
        repository.insertLine(listId, itemId, null, memberId, quantity, blankToNull(note));
        return get(listId);
    }

    /** Removes a row: every line of the item for that member, whether added by hand or by a recipe. */
    @Transactional
    public ShoppingList removeRow(long listId, long itemId, @Nullable Long memberId) {
        requireOpen(listId);
        if (repository.deleteRowLines(listId, itemId, memberId) == 0) {
            throw new NotFoundException("Article absent de la liste.");
        }
        repository.deletePurchase(listId, itemId, memberId);
        return get(listId);
    }

    /** Records what will be bought for a row, replacing the quantity and estimate computed from its lines. */
    @Transactional
    public ShoppingList setPurchase(long listId, long itemId, @Nullable Long memberId, Purchase purchase) {
        requireOpen(listId);
        if (!repository.rowExists(listId, itemId, memberId)) {
            throw new NotFoundException("Article absent de la liste.");
        }
        repository.savePurchase(listId, itemId, memberId,
                new Purchase(purchase.quantity(), purchase.priceCents(), blankToNull(purchase.note())));
        return get(listId);
    }

    /** Goes back to the quantity and estimate computed from the lines of a row. */
    @Transactional
    public ShoppingList clearPurchase(long listId, long itemId, @Nullable Long memberId) {
        requireOpen(listId);
        repository.deletePurchase(listId, itemId, memberId);
        return get(listId);
    }

    @Transactional
    public ShoppingList setChecked(long listId, long itemId, @Nullable Long memberId, boolean checked) {
        requireOpen(listId);
        if (repository.setRowChecked(listId, itemId, memberId, checked) == 0) {
            throw new NotFoundException("Article absent de la liste.");
        }
        return get(listId);
    }

    /**
     * Adds the recipe's ingredients, scaled to {@code servings}, as lines tied to this recipe entry, except
     * those in {@code skippedItemIds}.
     */
    @Transactional
    @WithSpan
    public ShoppingList addRecipe(@SpanAttribute("familyhub.shopping.list_id") long listId,
            @SpanAttribute("familyhub.recipe.id") long recipeId, int servings,
            Collection<Long> skippedItemIds) {
        requireOpen(listId);
        List<RecipeIngredient> ingredients = recipes.ingredientsFor(recipeId, servings);
        long entryId = repository.insertRecipe(listId, recipeId, servings);
        for (RecipeIngredient ingredient : ingredients) {
            if (skippedItemIds.contains(ingredient.itemId())) {
                continue;
            }
            repository.insertLine(listId, ingredient.itemId(), entryId, null, ingredient.quantity(), null);
        }
        return get(listId);
    }

    @Transactional
    public ShoppingList removeRecipe(long listId, long entryId) {
        requireOpen(listId);
        if (repository.deleteRecipe(listId, entryId) == 0) {
            throw new NotFoundException("Recette absente de la liste.");
        }
        repository.deleteOrphanPurchases(listId);
        return get(listId);
    }

    /**
     * Closes the list with what was paid in each kind of shop and publishes {@link ShoppingListCompleted},
     * whose listeners run in this transaction: the list and its consequences are saved together.
     */
    @Transactional
    @WithSpan
    public ShoppingList complete(@SpanAttribute("familyhub.shopping.list_id") long listId,
            List<ShoppingPayment> payments, @Nullable LocalDate paidOn, @Nullable Long accountId) {
        requireOpen(listId);
        if (payments.isEmpty()) {
            throw new InvalidRequestException("Indiquez au moins un montant payé.");
        }
        if (payments.stream().map(ShoppingPayment::storeType).distinct().count() != payments.size()) {
            throw new InvalidRequestException("Un seul montant par type de magasin.");
        }
        LocalDate day = paidOn == null ? LocalDate.now(clock) : paidOn;
        repository.complete(listId, day, payments.stream().mapToLong(ShoppingPayment::amountCents).sum());
        ShoppingList list = get(listId);
        events.publishEvent(new ShoppingListCompleted(listId, list.name(), day, accountId, List.copyOf(payments)));
        LOG.atInfo().addKeyValue("shopping.list_id", listId).addKeyValue("shopping.payments", payments.size())
                .log("Shopping list completed");
        return list;
    }

    /** Summaries of the given lists, for modules that reference lists. Unknown ids are absent. */
    public Map<Long, ShoppingListSummary> summaries(List<Long> ids) {
        return ids.stream().distinct()
                .filter(id -> repository.findList(id).isPresent())
                .map(id -> ShoppingListSummary.of(get(id)))
                .collect(Collectors.toMap(ShoppingListSummary::id, Function.identity()));
    }

    private ListRow findList(long id) {
        return repository.findList(id).orElseThrow(() -> new NotFoundException("Liste de courses introuvable."));
    }

    private void requireOpen(long id) {
        if (findList(id).status() != ShoppingStatus.OPEN) {
            throw new ConflictException("Cette liste de courses est terminée.");
        }
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
