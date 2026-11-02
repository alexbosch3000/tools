package io.github.alexbosch3000.dupscan.match;

/** Why a group of files is considered duplicate. */
public enum MatchType {
    /** Byte/content identical (same SHA-256). */
    EXACT,
    /** Visually near-identical (perceptual hash within tolerance). */
    NEAR
}
