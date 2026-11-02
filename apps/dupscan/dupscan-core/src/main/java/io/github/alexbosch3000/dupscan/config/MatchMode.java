package io.github.alexbosch3000.dupscan.config;

/** How duplicates are detected. */
public enum MatchMode {
    /** Only byte/content-identical files (streaming SHA-256). */
    EXACT,
    /** Only perceptual similarity (image hash within a tolerance). */
    PERCEPTUAL,
    /** Exact first, then perceptual near-duplicates. The default. */
    EXACT_PERCEPTUAL
}
