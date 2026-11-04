// A to-do item, optionally assigned, due at a minute-precision local date-time and repeating.
package io.github.alexbosch3000.familyhub.todo;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

public record TodoItem(long id, long listId, String title, String notes, @Nullable Long assigneeId,
        @Nullable LocalDateTime dueAt, Priority priority, @Nullable Repeat repeat, boolean done,
        @Nullable LocalDateTime doneAt) {
}
