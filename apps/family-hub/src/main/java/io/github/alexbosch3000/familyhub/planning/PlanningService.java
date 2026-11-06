// Public API of the planning module: entries, their occurrences, meal-based shopping and the calendar feed.
package io.github.alexbosch3000.familyhub.planning;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.planning.internal.FrenchHolidays;
import io.github.alexbosch3000.familyhub.planning.internal.IcsCalendar;
import io.github.alexbosch3000.familyhub.planning.internal.PlanningMetrics;
import io.github.alexbosch3000.familyhub.planning.internal.PlanningRepository;
import io.github.alexbosch3000.familyhub.planning.internal.RecurrenceExpander;
import io.github.alexbosch3000.familyhub.recipes.RecipeService;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import io.github.alexbosch3000.familyhub.shopping.ShoppingService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningService {

    private static final long MAX_RANGE_DAYS = 400;
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private final PlanningRepository repository;
    private final ShoppingService shopping;
    private final RecipeService recipes;
    private final MemberService members;
    private final ApplicationEventPublisher events;
    private final PlanningMetrics metrics;
    private final Clock clock;

    PlanningService(PlanningRepository repository, ShoppingService shopping, RecipeService recipes,
            MemberService members, ApplicationEventPublisher events, PlanningMetrics metrics, Clock clock) {
        this.repository = repository;
        this.shopping = shopping;
        this.recipes = recipes;
        this.members = members;
        this.events = events;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * Values of an entry to create or update. Fields not relevant to {@code type} are ignored.
     *
     * @param createShoppingList SHOPPING only: create a list named after the entry instead of referencing one
     */
    public record EntryData(String title, @Nullable String description, @Nullable String location, EntryType type,
            LocalDateTime start, LocalDateTime end, boolean allDay, @Nullable Recurrence recurrence,
            @Nullable List<Long> memberIds, @Nullable EventCategory category, @Nullable Long estimatedCostCents,
            @Nullable Long shoppingListId, boolean createShoppingList, @Nullable Long recipeId,
            @Nullable Integer servings, @Nullable AccountingAction accountingAction) {
    }

    /** Result of turning planned meals into a shopping list. */
    public record MealShopping(long shoppingListId, int meals) {
    }

    /** Occurrences overlapping {@code [from, to]}, chronologically. */
    @WithSpan
    public List<Occurrence> occurrences(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new InvalidRequestException("Période invalide (" + MAX_RANGE_DAYS + " jours au plus).");
        }
        return repository.findCandidates(from, to).stream()
                .flatMap(entry -> RecurrenceExpander.starts(entry.start(), entry.end(), entry.recurrence(),
                                new HashSet<>(entry.exceptions()), from, to).stream()
                        .map(start -> new Occurrence(start, start.plus(Duration.between(entry.start(),
                                entry.end())), entry)))
                .sorted(Comparator.comparing(Occurrence::start).thenComparing(o -> o.entry().title()))
                .toList();
    }

    public PlanningEntry get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Entrée de planning introuvable."));
    }

    @Transactional
    public PlanningEntry create(EntryData data) {
        PlanningEntry entry = normalize(0, data, List.of(), false);
        long id = repository.insert(entry, LocalDateTime.now(clock));
        metrics.created(entry.type());
        publishCost(get(id));
        return get(id);
    }

    @Transactional
    public PlanningEntry update(long id, EntryData data) {
        PlanningEntry existing = get(id);
        repository.update(id, normalize(id, data, existing.exceptions(), existing.done()));
        PlanningEntry updated = get(id);
        publishCost(updated);
        return updated;
    }

    @Transactional
    public void delete(long id) {
        get(id);
        repository.delete(id);
        events.publishEvent(new PlannedExpenseCancelled(id));
    }

    /** Cancels one occurrence of a recurring entry. */
    @Transactional
    public PlanningEntry skipOccurrence(long id, LocalDate day) {
        if (get(id).recurrence() == null) {
            throw new InvalidRequestException("Seule une entrée récurrente peut sauter une occurrence.");
        }
        repository.addException(id, day);
        return get(id);
    }

    @Transactional
    public PlanningEntry setDone(long id, boolean done) {
        get(id);
        repository.setDone(id, done);
        return get(id);
    }

    /**
     * Adds the recipe of every meal planned in {@code [from, to]} to a shopping list, scaled to the
     * meal's servings. Uses the given open list, or creates one.
     */
    @Transactional
    @WithSpan
    public MealShopping prepareShopping(LocalDate from, LocalDate to, @Nullable Long shoppingListId,
            @Nullable String name) {
        List<Occurrence> meals = occurrences(from, to).stream()
                .filter(o -> o.entry().type() == EntryType.MEAL && o.entry().recipeId() != null)
                .toList();
        if (meals.isEmpty()) {
            throw new InvalidRequestException("Aucun repas planifié sur cette période.");
        }
        long listId = shoppingListId != null ? shoppingListId : shopping.create(
                name == null || name.isBlank()
                        ? "Courses du " + DAY_MONTH.format(from) + " au " + DAY_MONTH.format(to) : name,
                from).id();
        for (Occurrence meal : meals) {
            shopping.addRecipe(listId, meal.entry().recipeId(), meal.entry().servings(), List.of());
        }
        return new MealShopping(listId, meals.size());
    }

    /** French public holidays of a year. */
    public Map<LocalDate, String> holidays(int year) {
        return FrenchHolidays.of(year);
    }

    /** The whole family planning as iCalendar, for the member owning the token; empty if the token is unknown. */
    public Optional<String> calendarFeed(String token) {
        return members.findByCalendarToken(token).map(member -> IcsCalendar.write(repository.findAll(),
                clock.getZone(), clock.instant(), "Famille"));
    }

    /** Closes the shopping entries of a completed list. */
    @Transactional
    public void closeShopping(long shoppingListId) {
        repository.markShoppingDone(shoppingListId);
    }

    public ZoneId zone() {
        return clock.getZone();
    }

    private void publishCost(PlanningEntry entry) {
        if (entry.type() == EntryType.EVENT && entry.estimatedCostCents() != null) {
            events.publishEvent(new PlannedExpenseChanged(entry.id(), entry.title(), entry.category(),
                    entry.start().toLocalDate(), entry.estimatedCostCents()));
        } else {
            events.publishEvent(new PlannedExpenseCancelled(entry.id()));
        }
    }

    /** Validates the data and keeps only the fields of its type. */
    private PlanningEntry normalize(long id, EntryData data, List<LocalDate> exceptions, boolean done) {
        LocalDateTime start = data.start().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = data.end().truncatedTo(ChronoUnit.MINUTES);
        if (data.allDay()) {
            start = start.toLocalDate().atStartOfDay();
            end = end.toLocalDate().atTime(LocalTime.of(23, 59));
        }
        if (end.isBefore(start)) {
            throw new InvalidRequestException("La fin doit suivre le début.");
        }
        Recurrence recurrence = validRecurrence(data.recurrence(), start);
        List<Long> memberIds = validMembers(data.memberIds());
        String title = data.title().trim();

        EventCategory category = null;
        Long cost = null;
        Long listId = null;
        Long recipeId = null;
        Integer servings = null;
        AccountingAction action = null;
        switch (data.type()) {
            case EVENT -> {
                category = data.category() == null ? EventCategory.OTHER : data.category();
                cost = data.estimatedCostCents();
            }
            case SHOPPING -> listId = shoppingList(data, title, start.toLocalDate());
            case MEAL -> {
                if (data.recipeId() == null) {
                    throw new InvalidRequestException("Choisissez une recette pour ce repas.");
                }
                recipeId = data.recipeId();
                int recipeServings = recipes.get(recipeId).servings();
                servings = data.servings() != null ? data.servings() : recipeServings;
            }
            case ACCOUNTING -> {
                if (data.accountingAction() == null) {
                    throw new InvalidRequestException("Choisissez l'action comptable.");
                }
                action = data.accountingAction();
            }
            default -> throw new IllegalStateException("Unknown type " + data.type());
        }
        return new PlanningEntry(id, title, data.description(), blankToNull(data.location()), data.type(), start, end,
                data.allDay(), recurrence, recurrence == null ? List.of() : exceptions, memberIds, category, cost,
                listId, recipeId, servings, action, done);
    }

    private Long shoppingList(EntryData data, String title, LocalDate day) {
        if (data.shoppingListId() != null) {
            return shopping.get(data.shoppingListId()).id();
        }
        if (data.createShoppingList()) {
            return shopping.create(title, day).id();
        }
        throw new InvalidRequestException("Choisissez ou créez une liste de courses.");
    }

    private static @Nullable Recurrence validRecurrence(@Nullable Recurrence recurrence, LocalDateTime start) {
        if (recurrence == null) {
            return null;
        }
        if (recurrence.interval() < 1 || recurrence.interval() > 99) {
            throw new InvalidRequestException("L'intervalle de répétition doit être compris entre 1 et 99.");
        }
        if (recurrence.until() != null && recurrence.until().isBefore(start.toLocalDate())) {
            throw new InvalidRequestException("La fin de répétition précède le début.");
        }
        if (recurrence.frequency() != Frequency.WEEKLY && !recurrence.days().isEmpty()) {
            throw new InvalidRequestException(
                    "Les jours de la semaine ne s'appliquent qu'à une répétition hebdomadaire.");
        }
        if (!recurrence.days().isEmpty() && !recurrence.days().contains(start.getDayOfWeek())) {
            throw new InvalidRequestException("Le jour du début doit faire partie des jours de répétition.");
        }
        return recurrence;
    }

    private List<Long> validMembers(@Nullable List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> known = members.list().stream().map(Member::id).collect(Collectors.toSet());
        List<Long> distinct = ids.stream().distinct().sorted().toList();
        if (!known.containsAll(distinct)) {
            throw new InvalidRequestException("Membre inconnu.");
        }
        return distinct;
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
