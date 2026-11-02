package io.github.alexbosch3000.dupscan.config;

/** Perceptual hashing algorithm. Both produce a 64-bit hash compared by Hamming distance. */
public enum HashAlgorithm {
    /** Difference hash: fast, robust to scaling/compression. The default. */
    DHASH,
    /** Perceptual hash via DCT: more robust to minor edits, slightly slower. */
    PHASH
}
