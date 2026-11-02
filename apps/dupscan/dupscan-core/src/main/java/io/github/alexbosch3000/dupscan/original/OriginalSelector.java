package io.github.alexbosch3000.dupscan.original;

import io.github.alexbosch3000.dupscan.config.OriginalRule;
import io.github.alexbosch3000.dupscan.index.ImageRecord;

import java.util.Comparator;
import java.util.List;

/**
 * Picks which member of a duplicate group is the "original". Pluggable so the rule can change
 * later without touching matching or reporting. The chosen file is kept; the rest are
 * reported as duplicates.
 */
@FunctionalInterface
public interface OriginalSelector {

    ImageRecord select(List<ImageRecord> members);

    static OriginalSelector of(OriginalRule rule) {
        return switch (rule) {
            case HIGHEST_QUALITY -> highestQuality();
            case OLDEST -> oldest();
            case SHORTEST_PATH -> shortestPath();
        };
    }

    /** Largest resolution, then largest bytes, then oldest, then path for determinism. */
    static OriginalSelector highestQuality() {
        Comparator<ImageRecord> best = Comparator
                .comparingLong(ImageRecord::pixels)
                .thenComparingLong(ImageRecord::sizeBytes)
                .thenComparing(Comparator.comparingLong(ImageRecord::effectiveDateMillis).reversed())
                .thenComparing(ImageRecord::canonicalPath, Comparator.reverseOrder());
        return members -> members.stream().max(best).orElseThrow();
    }

    /** Earliest known date, then path for determinism. */
    static OriginalSelector oldest() {
        Comparator<ImageRecord> earliest = Comparator
                .comparingLong(ImageRecord::effectiveDateMillis)
                .thenComparing(ImageRecord::canonicalPath);
        return members -> members.stream().min(earliest).orElseThrow();
    }

    /** Shortest/cleanest path (fewest segments, then shortest string). */
    static OriginalSelector shortestPath() {
        Comparator<ImageRecord> cleanest = Comparator
                .comparingInt((ImageRecord r) -> r.canonicalPath().split("/").length)
                .thenComparingInt(r -> r.canonicalPath().length())
                .thenComparing(ImageRecord::canonicalPath);
        return members -> members.stream().min(cleanest).orElseThrow();
    }
}
