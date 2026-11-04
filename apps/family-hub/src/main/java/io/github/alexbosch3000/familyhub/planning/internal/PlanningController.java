// Planning API: entries, occurrences over a period, public holidays and meal-based shopping lists.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.AccountingAction;
import io.github.alexbosch3000.familyhub.planning.EntryType;
import io.github.alexbosch3000.familyhub.planning.EventCategory;
import io.github.alexbosch3000.familyhub.planning.Frequency;
import io.github.alexbosch3000.familyhub.planning.Occurrence;
import io.github.alexbosch3000.familyhub.planning.PlanningEntry;
import io.github.alexbosch3000.familyhub.planning.PlanningService;
import io.github.alexbosch3000.familyhub.planning.PlanningService.EntryData;
import io.github.alexbosch3000.familyhub.planning.PlanningService.MealShopping;
import io.github.alexbosch3000.familyhub.planning.Recurrence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planning")
class PlanningController {

    private final PlanningService planning;

    PlanningController(PlanningService planning) {
        this.planning = planning;
    }

    record RecurrenceRequest(@NotNull Frequency frequency, @Min(1) @Max(99) int interval,
            @Nullable List<DayOfWeek> days, @Nullable LocalDate until) {
    }

    record EntryRequest(
            @NotBlank @Size(max = 120) String title,
            @Nullable @Size(max = 4000) String description,
            @Nullable @Size(max = 200) String location,
            @NotNull EntryType type,
            @NotNull LocalDateTime start,
            @NotNull LocalDateTime end,
            boolean allDay,
            @Nullable @Valid RecurrenceRequest recurrence,
            @Nullable @Size(max = 20) List<Long> memberIds,
            @Nullable EventCategory category,
            @Nullable @PositiveOrZero Long estimatedCostCents,
            @Nullable Long shoppingListId,
            boolean createShoppingList,
            @Nullable Long recipeId,
            @Nullable @Min(1) @Max(50) Integer servings,
            @Nullable AccountingAction accountingAction) {

        EntryData toData() {
            Recurrence rule = recurrence == null ? null
                    : new Recurrence(recurrence.frequency(), recurrence.interval(), recurrence.days(),
                            recurrence.until());
            return new EntryData(title, description, location, type, start, end, allDay, rule, memberIds, category,
                    estimatedCostCents, shoppingListId, createShoppingList, recipeId, servings, accountingAction);
        }
    }

    record DayRequest(@NotNull LocalDate day) {
    }

    record EntryDoneRequest(boolean done) {
    }

    record MealShoppingRequest(@NotNull LocalDate from, @NotNull LocalDate to, @Nullable Long shoppingListId,
            @Nullable @Size(max = 80) String name) {
    }

    record Holiday(LocalDate day, String name) {
    }

    @GetMapping("/occurrences")
    List<Occurrence> occurrences(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return planning.occurrences(from, to);
    }

    @GetMapping("/entries/{id}")
    PlanningEntry get(@PathVariable long id) {
        return planning.get(id);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    PlanningEntry create(@Valid @RequestBody EntryRequest request) {
        return planning.create(request.toData());
    }

    @PutMapping("/entries/{id}")
    PlanningEntry update(@PathVariable long id, @Valid @RequestBody EntryRequest request) {
        return planning.update(id, request.toData());
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        planning.delete(id);
    }

    @PostMapping("/entries/{id}/exceptions")
    PlanningEntry skip(@PathVariable long id, @Valid @RequestBody DayRequest request) {
        return planning.skipOccurrence(id, request.day());
    }

    @PutMapping("/entries/{id}/done")
    PlanningEntry done(@PathVariable long id, @RequestBody EntryDoneRequest request) {
        return planning.setDone(id, request.done());
    }

    @PostMapping("/meals/shopping")
    MealShopping mealShopping(@Valid @RequestBody MealShoppingRequest request) {
        return planning.prepareShopping(request.from(), request.to(), request.shoppingListId(), request.name());
    }

    @GetMapping("/holidays")
    List<Holiday> holidays(@RequestParam @Min(1900) @Max(2200) int year) {
        return planning.holidays(year).entrySet().stream().map(e -> new Holiday(e.getKey(), e.getValue())).toList();
    }
}
