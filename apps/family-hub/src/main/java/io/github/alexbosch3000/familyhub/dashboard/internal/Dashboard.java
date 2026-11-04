// Everything the home page shows, computed for the logged-in member.
package io.github.alexbosch3000.familyhub.dashboard.internal;

import io.github.alexbosch3000.familyhub.accounting.AccountingSummary;
import io.github.alexbosch3000.familyhub.planning.Occurrence;
import io.github.alexbosch3000.familyhub.shopping.ShoppingListSummary;
import io.github.alexbosch3000.familyhub.todo.TodoItem;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @param agenda      occurrences of the next seven days
 * @param load        planning entries and open to-dos per member over the same week (mental load indicator)
 * @param accounting  null for children
 */
record Dashboard(LocalDate today, List<Occurrence> agenda, List<MemberLoad> load, List<TodoItem> overdueTodos,
        List<TodoItem> dueSoonTodos, int plannedMeals, List<ShoppingListSummary> shoppingLists,
        @Nullable AccountingSummary accounting) {

    record MemberLoad(long memberId, String name, String color, int entries, int todos) {
    }
}
