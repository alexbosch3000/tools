package io.github.alexbosch3000.dupscan.config;

/** Strategy for picking which file in a duplicate group is the "original". */
public enum OriginalRule {
    /** Highest resolution, then largest bytes, then oldest. The default. */
    HIGHEST_QUALITY,
    /** Earliest known date (EXIF, else last-modified). */
    OLDEST,
    /** Shortest/cleanest canonical path. */
    SHORTEST_PATH
}
