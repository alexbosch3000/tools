// A planning entry: a time slot at minute precision, its attendees, its recurrence and its type-specific links.
package io.github.alexbosch3000.familyhub.planning;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Date-times are local to the family's time zone. Fields of other types than {@code type} are null.
 *
 * @param end                inclusive end; for all-day entries the last day at 23:59
 * @param recurrence         null for a one-off entry
 * @param exceptions         days on which a recurring entry does not take place
 * @param category           EVENT only
 * @param estimatedCostCents EVENT only, forwarded to the budget as a planned expense
 * @param shoppingListId     SHOPPING only
 * @param recipeId           MEAL only
 * @param servings           MEAL only
 * @param accountingAction   ACCOUNTING only
 */
public record PlanningEntry(long id, String title, String description, @Nullable String location, EntryType type,
        LocalDateTime start, LocalDateTime end, boolean allDay, @Nullable Recurrence recurrence,
        List<LocalDate> exceptions, List<Long> memberIds, @Nullable EventCategory category,
        @Nullable Long estimatedCostCents, @Nullable Long shoppingListId, @Nullable Long recipeId,
        @Nullable Integer servings, @Nullable AccountingAction accountingAction, boolean done) {
}
