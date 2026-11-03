// Public API of the shopping module: lists, their items and recipes, and completion of a shopping trip.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.CatalogService;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.recipes.RecipeIngredient;
import io.github.alexbosch3000.familyhub.recipes.RecipeService;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingLine;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRepository;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRepository.ListRow;
import io.github.alexbosch3000.familyhub.shopping.internal.ShoppingRows;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingService {

    private static final int DONE_LISTS_SHOWN = 20;

    private final ShoppingRepository repository;
    private final CatalogService catalog;
    private final RecipeService recipes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ShoppingService(ShoppingRepository repository, CatalogService catalog, RecipeService recipes,
            ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.recipes = recipes;
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
        List<ShoppingRow> rows = ShoppingRows.build(lines, items, recipeNames);
        long estimated = rows.stream().map(ShoppingRow::estimatedCents).filter(Objects::nonNull)
                .mapToLong(Long::longValue).sum();
        int unpriced = (int) rows.stream().filter(r -> r.estimatedCents() == null).count();
        return new ShoppingList(list.id(), list.name(), list.status(), list.plannedFor(), list.completedOn(),
                list.amountPaidCents(), rows, listed, estimated, unpriced);
    }

    @Transactional
    public ShoppingList create(String name, LocalDate plannedFor) {
        return get(repository.insertList(name, plannedFor, LocalDateTime.now(clock)));
    }

    @Transactional
    public ShoppingList update(long id, String name, LocalDate plannedFor) {
        findList(id);
        repository.updateList(id, name, plannedFor);
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        findList(id);
        repository.deleteList(id);
    }

    @Transactional
    public ShoppingList addItem(long listId, long itemId, Quantity quantity, String note) {
        requireOpen(listId);
        catalog.get(itemId);
        repository.insertLine(listId, itemId, null, quantity, blankToNull(note));
        return get(listId);
    }

    /** Removes every line of the item, whether added by hand or by a recipe. */
    @Transactional
    public ShoppingList removeItem(long listId, long itemId) {
        requireOpen(listId);
        if (repository.deleteItemLines(listId, itemId) == 0) {
            throw new NotFoundException("Article absent de la liste.");
        }
        return get(listId);
    }

    @Transactional
    public ShoppingList setChecked(long listId, long itemId, boolean checked) {
        requireOpen(listId);
        if (repository.setChecked(listId, itemId, checked) == 0) {
            throw new NotFoundException("Article absent de la liste.");
        }
        return get(listId);
    }

    /** Adds the recipe's ingredients, scaled to {@code servings}, as lines tied to this recipe entry. */
    @Transactional
    public ShoppingList addRecipe(long listId, long recipeId, int servings) {
        requireOpen(listId);
        List<RecipeIngredient> ingredients = recipes.ingredientsFor(recipeId, servings);
        long entryId = repository.insertRecipe(listId, recipeId, servings);
        for (RecipeIngredient ingredient : ingredients) {
            repository.insertLine(listId, ingredient.itemId(), entryId, ingredient.quantity(), null);
        }
        return get(listId);
    }

    @Transactional
    public ShoppingList removeRecipe(long listId, long entryId) {
        requireOpen(listId);
        if (repository.deleteRecipe(listId, entryId) == 0) {
            throw new NotFoundException("Recette absente de la liste.");
        }
        return get(listId);
    }

    /**
     * Closes the list with the amount actually paid and publishes {@link ShoppingListCompleted}, whose
     * listeners run in this transaction: the list and its consequences are saved together.
     */
    @Transactional
    public ShoppingList complete(long listId, long amountPaidCents, LocalDate paidOn, Long accountId) {
        requireOpen(listId);
        LocalDate day = paidOn == null ? LocalDate.now(clock) : paidOn;
        repository.complete(listId, day, amountPaidCents);
        ShoppingList list = get(listId);
        events.publishEvent(new ShoppingListCompleted(listId, list.name(), day, amountPaidCents, accountId,
                ShoppingRows.mainStoreType(list.rows())));
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
