package io.github.alexbosch3000.dupscan.pipeline;

import io.github.alexbosch3000.dupscan.config.ScanConfig;
import io.github.alexbosch3000.dupscan.hash.ContentHasher;
import io.github.alexbosch3000.dupscan.hash.DecodeGuard;
import io.github.alexbosch3000.dupscan.hash.PerceptualHasher;
import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.index.IndexStore;
import io.github.alexbosch3000.dupscan.metadata.MetadataReader;
import io.github.alexbosch3000.dupscan.scan.ImageFileFilter;
import io.github.alexbosch3000.dupscan.scan.WorkSource;
import io.github.alexbosch3000.dupscan.service.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded producer/worker/single-writer pipeline. The {@link WorkSource} produces items onto
 * a bounded queue (backpressure keeps memory flat); a fixed worker pool decodes and hashes in
 * parallel; one writer thread batches transactional upserts into the {@link IndexStore}.
 *
 * <p>Safety: writes go through a single writer with idempotent, per-path upserts, so retries
 * and re-runs never double-count and every committed batch leaves the index consistent and
 * resumable. One unreadable/oversized file is skipped and logged, never aborting the run.
 */
public final class HashingPipeline {

    private static final Logger log = LoggerFactory.getLogger(HashingPipeline.class);
    private static final int WRITE_BATCH = 500;
    private static final WorkItem WORKER_POISON = new WorkItem(null, -1, -1);

    private final ScanConfig config;
    private final WorkSource workSource;
    private final IndexStore index;
    private final ContentHasher contentHasher;
    private final PerceptualHasher perceptualHasher;
    private final DecodeGuard decodeGuard;
    private final MetadataReader metadataReader;
    private final ProgressListener progress;

    public HashingPipeline(ScanConfig config, WorkSource workSource, IndexStore index,
                           ContentHasher contentHasher, PerceptualHasher perceptualHasher,
                           DecodeGuard decodeGuard, MetadataReader metadataReader,
                           ProgressListener progress) {
        this.config = config;
        this.workSource = workSource;
        this.index = index;
        this.contentHasher = contentHasher;
        this.perceptualHasher = perceptualHasher;
        this.decodeGuard = decodeGuard;
        this.metadataReader = metadataReader;
        this.progress = progress;
    }

    public record Stats(long discovered, long created, long reused, long skipped) {
    }

    /** Outcome of processing one file. Exactly one of the cases applies. */
    private record Outcome(ImageRecord created, String reusedPath, boolean skipped) {
        static final Outcome POISON = new Outcome(null, null, false);

        static Outcome created(ImageRecord r) {
            return new Outcome(r, null, false);
        }

        static Outcome reused(String path) {
            return new Outcome(null, path, false);
        }

        static Outcome skip() {
            return new Outcome(null, null, true);
        }
    }

    public Stats run(long runId) throws InterruptedException {
        int threads = config.threads();
        BlockingQueue<WorkItem> workQueue = new ArrayBlockingQueue<>(threads * 8);
        BlockingQueue<Outcome> outcomeQueue = new ArrayBlockingQueue<>(threads * 8);

        AtomicLong discovered = new AtomicLong();
        AtomicLong created = new AtomicLong();
        AtomicLong reused = new AtomicLong();
        AtomicLong skipped = new AtomicLong();

        Thread writer = new Thread(() ->
                writerLoop(outcomeQueue, runId, created, reused, skipped, discovered), "dupscan-writer");
        writer.start();

        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setName("dupscan-worker");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> workerLoop(workQueue, outcomeQueue));
        }

        progress.onPhase("Scanning and hashing");
        try {
            long n = workSource.produce(item -> {
                discovered.incrementAndGet();
                putUninterruptibly(workQueue, item);
            });
            log.info("Discovered {} candidate files", n);
        } catch (Exception e) {
            log.error("Walk failed: {}", e.toString(), e);
        } finally {
            for (int i = 0; i < threads; i++) {
                workQueue.put(WORKER_POISON);
            }
        }

        pool.shutdown();
        if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
            log.warn("Worker pool did not terminate in time");
        }
        outcomeQueue.put(Outcome.POISON);
        writer.join();

        return new Stats(discovered.get(), created.get(), reused.get(), skipped.get());
    }

    private void workerLoop(BlockingQueue<WorkItem> in, BlockingQueue<Outcome> out) {
        try {
            while (true) {
                WorkItem item = in.take();
                if (item == WORKER_POISON) {
                    return;
                }
                Outcome outcome;
                try {
                    outcome = process(item);
                } catch (Exception e) {
                    log.warn("Skipping {}: {}", item.path(), e.toString());
                    outcome = Outcome.skip();
                }
                out.put(outcome);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Outcome process(WorkItem item) throws Exception {
        String canonical = item.path().toString();

        if (!config.rehash()) {
            Optional<ImageRecord> existing = index.lookup(canonical);
            if (existing.isPresent()
                    && existing.get().sizeBytes() == item.sizeBytes()
                    && existing.get().modifiedMillis() == item.modifiedMillis()) {
                return Outcome.reused(canonical);
            }
        }

        if (!decodeGuard.fileWithinByteLimit(item.sizeBytes())) {
            log.warn("Skipping oversized file ({} bytes): {}", item.sizeBytes(), item.path());
            return Outcome.skip();
        }

        String sha = contentHasher.sha256(item.path());

        int width = 0;
        int height = 0;
        String format = ImageFileFilter.extensionOf(item.path());
        boolean withinPixelLimit = true;
        Optional<DecodeGuard.Dimensions> dims = decodeGuard.readDimensions(item.path());
        if (dims.isPresent()) {
            width = dims.get().width();
            height = dims.get().height();
            if (dims.get().formatName() != null) {
                format = dims.get().formatName().toLowerCase();
            }
            withinPixelLimit = decodeGuard.pixelsWithinLimit(dims.get().pixels());
        }

        Long perceptualHash = null;
        if (config.wantsPerceptual() && withinPixelLimit) {
            try {
                perceptualHash = perceptualHasher.hash(item.path()).orElse(null);
            } catch (Exception e) {
                log.debug("No perceptual hash for {}: {}", item.path(), e.toString());
            }
        }

        Long exif = metadataReader.readCaptureDateMillis(item.path()).orElse(null);

        ImageRecord record = new ImageRecord(canonical, item.sizeBytes(), item.modifiedMillis(),
                sha, perceptualHash, width, height, format, exif);
        return Outcome.created(record);
    }

    private void writerLoop(BlockingQueue<Outcome> queue, long runId, AtomicLong created,
                            AtomicLong reused, AtomicLong skipped, AtomicLong discovered) {
        List<ImageRecord> batch = new ArrayList<>(WRITE_BATCH);
        List<String> seen = new ArrayList<>(WRITE_BATCH);
        long processed = 0;
        try {
            while (true) {
                Outcome o = queue.take();
                if (o == Outcome.POISON) {
                    break;
                }
                if (o.created() != null) {
                    batch.add(o.created());
                    seen.add(o.created().canonicalPath());
                    created.incrementAndGet();
                } else if (o.reusedPath() != null) {
                    seen.add(o.reusedPath());
                    reused.incrementAndGet();
                } else {
                    skipped.incrementAndGet();
                }
                processed++;
                if (batch.size() >= WRITE_BATCH || seen.size() >= WRITE_BATCH) {
                    flush(batch, seen, runId);
                }
                if (processed % 1000 == 0) {
                    progress.onHashProgress(processed, discovered.get());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            flush(batch, seen, runId);
            progress.onHashProgress(processed, discovered.get());
        }
    }

    private void flush(List<ImageRecord> batch, List<String> seen, long runId) {
        if (!batch.isEmpty()) {
            index.upsertBatch(batch);
            batch.clear();
        }
        if (!seen.isEmpty()) {
            index.markSeen(seen, runId);
            seen.clear();
        }
    }

    private static <T> void putUninterruptibly(BlockingQueue<T> queue, T item) {
        boolean interrupted = false;
        while (true) {
            try {
                queue.put(item);
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
