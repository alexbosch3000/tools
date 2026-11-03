// Shopping list as shown in lists and on the dashboard.
package io.github.alexbosch3000.familyhub.shopping;

import java.time.LocalDate;

public record ShoppingListSummary(long id, String name, ShoppingStatus status, LocalDate plannedFor,
        LocalDate completedOn, Long amountPaidCents, int itemCount, int checkedCount, long estimatedTotalCents) {

    static ShoppingListSummary of(ShoppingList list) {
        return new ShoppingListSummary(list.id(), list.name(), list.status(), list.plannedFor(), list.completedOn(),
                list.amountPaidCents(), list.rows().size(), list.checkedCount(), list.estimatedTotalCents());
    }
}
