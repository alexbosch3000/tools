// Dashboard API: a read-only aggregation of the other modules for the coming week.
package io.github.alexbosch3000.familyhub.dashboard.internal;

import io.github.alexbosch3000.familyhub.accounting.AccountingService;
import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.family.Role;
import io.github.alexbosch3000.familyhub.planning.EntryType;
import io.github.alexbosch3000.familyhub.planning.Occurrence;
import io.github.alexbosch3000.familyhub.planning.PlanningService;
import io.github.alexbosch3000.familyhub.shopping.ShoppingService;
import io.github.alexbosch3000.familyhub.shopping.ShoppingStatus;
import io.github.alexbosch3000.familyhub.todo.TodoItem;
import io.github.alexbosch3000.familyhub.todo.TodoService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DashboardController {

    private static final int WEEK_DAYS = 7;
    private static final int DUE_SOON_HOURS = 48;

    private final PlanningService planning;
    private final TodoService todos;
    private final ShoppingService shopping;
    private final AccountingService accounting;
    private final MemberService members;
    private final Clock clock;

    DashboardController(PlanningService planning, TodoService todos, ShoppingService shopping,
            AccountingService accounting, MemberService members, Clock clock) {
        this.planning = planning;
        this.todos = todos;
        this.shopping = shopping;
        this.accounting = accounting;
        this.members = members;
        this.clock = clock;
    }

    @GetMapping("/api/dashboard")
    Dashboard dashboard() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        List<Occurrence> week = planning.occurrences(today, today.plusDays(WEEK_DAYS - 1));
        List<TodoItem> open = todos.openItems();
        List<Dashboard.MemberLoad> load = members.list().stream()
                .map(m -> new Dashboard.MemberLoad(m.id(), m.name(), m.color(),
                        (int) week.stream().filter(o -> o.entry().memberIds().contains(m.id())).count(),
                        (int) open.stream().filter(t -> Long.valueOf(m.id()).equals(t.assigneeId())).count()))
                .toList();
        List<TodoItem> overdue = open.stream().filter(t -> t.dueAt() != null && t.dueAt().isBefore(now)).toList();
        List<TodoItem> dueSoon = open.stream()
                .filter(t -> t.dueAt() != null && !t.dueAt().isBefore(now)
                        && t.dueAt().isBefore(now.plusHours(DUE_SOON_HOURS)))
                .toList();
        int meals = (int) week.stream().filter(o -> o.entry().type() == EntryType.MEAL).count();
        Member me = members.current();
        return new Dashboard(today, week, load, overdue, dueSoon, meals, shopping.list(ShoppingStatus.OPEN),
                me.role() == Role.PARENT ? accounting.summary() : null);
    }
}
