package io.github.alexbosch3000.dupscan.service;

import io.github.alexbosch3000.dupscan.config.ScanConfig;
import io.github.alexbosch3000.dupscan.hash.ContentHasher;
import io.github.alexbosch3000.dupscan.hash.DecodeGuard;
import io.github.alexbosch3000.dupscan.hash.PerceptualHasher;
import io.github.alexbosch3000.dupscan.hash.PreviewExtractor;
import io.github.alexbosch3000.dupscan.index.IndexStore;
import io.github.alexbosch3000.dupscan.index.SqliteIndexStore;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.match.DuplicateMatcher;
import io.github.alexbosch3000.dupscan.metadata.MetadataReader;
import io.github.alexbosch3000.dupscan.original.OriginalSelector;
import io.github.alexbosch3000.dupscan.pipeline.HashingPipeline;
import io.github.alexbosch3000.dupscan.scan.FileSystemWorkSource;
import io.github.alexbosch3000.dupscan.scan.ImageFileFilter;
import io.github.alexbosch3000.dupscan.scan.RootNormalizer;
import io.github.alexbosch3000.dupscan.scan.WorkSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The engine's single entry point. The CLI calls this today; a future backend API can call
 * the same facade and swap in distributed {@link WorkSource}/{@link IndexStore}
 * implementations without changing scanning, hashing or matching.
 */
public final class DuplicateScanService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateScanService.class);

    public ScanResult run(ScanConfig config, ProgressListener progress)
            throws IOException, InterruptedException {
        long start = System.currentTimeMillis();

        progress.onPhase("Resolving roots");
        List<Path> roots = RootNormalizer.normalize(config.roots());
        log.info("Scanning {} root(s): {}", roots.size(), roots);

        ImageFileFilter filter =
                new ImageFileFilter(config.formats(), config.includeGlobs(), config.excludeGlobs());
        WorkSource workSource = new FileSystemWorkSource(
                roots, filter, config.minSizeBytes(), config.followSymlinks());

        ensureParentDir(config.indexPath());

        try (IndexStore index = new SqliteIndexStore(config.indexPath())) {
            index.initialize();

            long runId = System.currentTimeMillis();
            HashingPipeline pipeline = new HashingPipeline(
                    config, workSource, index,
                    new ContentHasher(),
                    new PerceptualHasher(config.hashAlgorithm(), new PreviewExtractor()),
                    new DecodeGuard(config.maxPixels(), config.maxBytes()),
                    new MetadataReader(),
                    progress);

            HashingPipeline.Stats stats = pipeline.run(runId);

            long pruned = 0;
            if (config.prune()) {
                progress.onPhase("Pruning vanished files");
                List<String> prefixes = roots.stream().map(Path::toString).toList();
                pruned = index.pruneNotSeen(runId, prefixes);
                log.info("Pruned {} stale index rows", pruned);
            }

            progress.onPhase("Matching duplicates");
            DuplicateMatcher matcher =
                    new DuplicateMatcher(config, OriginalSelector.of(config.originalRule()));
            List<DuplicateGroup> groups = matcher.findGroups(index);

            long elapsed = System.currentTimeMillis() - start;
            return new ScanResult(groups, stats.discovered(), stats.created(),
                    stats.reused(), stats.skipped(), pruned, elapsed);
        }
    }

    private static void ensureParentDir(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
