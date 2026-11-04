// To-do metrics: items completed, and open items past their due time.
package io.github.alexbosch3000.familyhub.todo.internal;

import io.github.alexbosch3000.familyhub.shared.Telemetry;
import io.github.alexbosch3000.familyhub.todo.TodoItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TodoMetrics {

    private final Counter completed;

    TodoMetrics(TodoRepository repository, Clock clock) {
        Gauge.builder(Telemetry.PREFIX + "todo.items.overdue", () -> {
            LocalDateTime now = LocalDateTime.now(clock);
            return repository.findItems(null, null, false).stream()
                    .map(TodoItem::dueAt).filter(due -> due != null && due.isBefore(now)).count();
        }).description("Open to-do items past their due time").register(Telemetry.registry());
        completed = Counter.builder(Telemetry.PREFIX + "todo.items.completed").description("To-do items completed")
                .register(Telemetry.registry());
    }

    public void completed() {
        completed.increment();
    }
}
