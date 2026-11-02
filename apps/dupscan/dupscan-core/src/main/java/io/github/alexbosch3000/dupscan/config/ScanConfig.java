package io.github.alexbosch3000.dupscan.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration for a single scan run. Built by the front-end (CLI today, API
 * later) and consumed by {@code DuplicateScanService}. Use {@link #builder()} for defaults.
 */
public record ScanConfig(
        List<Path> roots,
        List<String> includeGlobs,
        List<String> excludeGlobs,
        Set<String> formats,
        long minSizeBytes,
        boolean followSymlinks,
        MatchMode mode,
        double threshold,
        HashAlgorithm hashAlgorithm,
        OriginalRule originalRule,
        Path indexPath,
        boolean rehash,
        boolean prune,
        Path reportXlsx,
        Path reportCsv,
        int topGroups,
        boolean showTable,
        int threads,
        long maxPixels,
        long maxBytes) {

    /** Image extensions recognised by default (lower-case, no dot). */
    public static final Set<String> DEFAULT_FORMATS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "webp",
            "heic", "heif",
            "cr2", "cr3", "nef", "arw", "dng", "raf", "rw2", "orf", "pef", "srw");

    public ScanConfig {
        Objects.requireNonNull(roots, "roots");
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("At least one root path is required");
        }
        roots = List.copyOf(roots);
        includeGlobs = List.copyOf(includeGlobs == null ? List.of() : includeGlobs);
        excludeGlobs = List.copyOf(excludeGlobs == null ? List.of() : excludeGlobs);
        formats = Set.copyOf(formats == null || formats.isEmpty() ? DEFAULT_FORMATS : formats);
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be in [0.0, 1.0]");
        }
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1");
        }
        mode = Objects.requireNonNull(mode, "mode");
        hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "hashAlgorithm");
        originalRule = Objects.requireNonNull(originalRule, "originalRule");
        indexPath = Objects.requireNonNull(indexPath, "indexPath");
    }

    public boolean wantsExact() {
        return mode == MatchMode.EXACT || mode == MatchMode.EXACT_PERCEPTUAL;
    }

    public boolean wantsPerceptual() {
        return mode == MatchMode.PERCEPTUAL || mode == MatchMode.EXACT_PERCEPTUAL;
    }

    /** Maximum Hamming distance (in bits, over 64) implied by {@link #threshold()}. */
    public int maxHammingDistance() {
        return (int) Math.round(threshold * 64.0);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder supplying sensible defaults for everything except the roots. */
    public static final class Builder {
        private List<Path> roots = List.of();
        private List<String> includeGlobs = List.of();
        private List<String> excludeGlobs = List.of();
        private Set<String> formats = DEFAULT_FORMATS;
        private long minSizeBytes = 0;
        private boolean followSymlinks = false;
        private MatchMode mode = MatchMode.EXACT_PERCEPTUAL;
        private double threshold = 0.15;
        private HashAlgorithm hashAlgorithm = HashAlgorithm.DHASH;
        private OriginalRule originalRule = OriginalRule.HIGHEST_QUALITY;
        private Path indexPath = Path.of(System.getProperty("user.home"), ".dupscan", "index.db");
        private boolean rehash = false;
        private boolean prune = false;
        private Path reportXlsx;
        private Path reportCsv;
        private int topGroups = 50;
        private boolean showTable = true;
        private int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        private long maxPixels = 268_435_456L; // 256 MP guard against decode bombs
        private long maxBytes = 2L * 1024 * 1024 * 1024; // 2 GiB per-file guard

        public Builder roots(List<Path> v) { this.roots = v; return this; }
        public Builder includeGlobs(List<String> v) { this.includeGlobs = v; return this; }
        public Builder excludeGlobs(List<String> v) { this.excludeGlobs = v; return this; }
        public Builder formats(Set<String> v) { this.formats = v; return this; }
        public Builder minSizeBytes(long v) { this.minSizeBytes = v; return this; }
        public Builder followSymlinks(boolean v) { this.followSymlinks = v; return this; }
        public Builder mode(MatchMode v) { this.mode = v; return this; }
        public Builder threshold(double v) { this.threshold = v; return this; }
        public Builder hashAlgorithm(HashAlgorithm v) { this.hashAlgorithm = v; return this; }
        public Builder originalRule(OriginalRule v) { this.originalRule = v; return this; }
        public Builder indexPath(Path v) { this.indexPath = v; return this; }
        public Builder rehash(boolean v) { this.rehash = v; return this; }
        public Builder prune(boolean v) { this.prune = v; return this; }
        public Builder reportXlsx(Path v) { this.reportXlsx = v; return this; }
        public Builder reportCsv(Path v) { this.reportCsv = v; return this; }
        public Builder topGroups(int v) { this.topGroups = v; return this; }
        public Builder showTable(boolean v) { this.showTable = v; return this; }
        public Builder threads(int v) { this.threads = v; return this; }
        public Builder maxPixels(long v) { this.maxPixels = v; return this; }
        public Builder maxBytes(long v) { this.maxBytes = v; return this; }

        public ScanConfig build() {
            return new ScanConfig(roots, includeGlobs, excludeGlobs, formats, minSizeBytes,
                    followSymlinks, mode, threshold, hashAlgorithm, originalRule, indexPath,
                    rehash, prune, reportXlsx, reportCsv, topGroups, showTable, threads,
                    maxPixels, maxBytes);
        }
    }
}
