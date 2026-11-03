// A to-do item, optionally assigned, due at a minute-precision local date-time and repeating.
package io.github.alexbosch3000.familyhub.todo;

import java.time.LocalDateTime;

public record TodoItem(long id, long listId, String title, String notes, Long assigneeId, LocalDateTime dueAt,
        Priority priority, Repeat repeat, boolean done, LocalDateTime doneAt) {
}
