// Domain event: an entry no longer carries a cost (cost removed, type changed or entry deleted).
package io.github.alexbosch3000.familyhub.planning;

public record PlannedExpenseCancelled(long entryId) {
}
