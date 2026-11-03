// One occurrence of a planning entry in a date range: the entry itself or one repetition of it.
package io.github.alexbosch3000.familyhub.planning;

import java.time.LocalDateTime;

public record Occurrence(LocalDateTime start, LocalDateTime end, PlanningEntry entry) {
}
