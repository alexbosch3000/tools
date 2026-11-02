package io.github.alexbosch3000.dupscan.index;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Persistent store of indexed images. v1 is backed by SQLite; this interface is the seam a
 * future backend can re-implement against a shared networked database (e.g. Postgres) so the
 * scan/match logic can run across machines unchanged.
 *
 * <p>Implementations must be safe for concurrent {@link #lookup} reads while a single writer
 * performs {@link #upsertBatch}. Writes are idempotent on {@code canonicalPath}.
 */
public interface IndexStore extends AutoCloseable {

    void initialize();

    Optional<ImageRecord> lookup(String canonicalPath);

    /** Idempotent upsert of a batch within one transaction. */
    void upsertBatch(List<ImageRecord> records);

    /** Records the paths seen in the current run, for {@link #pruneNotSeen}. */
    void markSeen(List<String> canonicalPaths, long runId);

    /**
     * Deletes index rows under {@code rootPrefixes} that were not seen in {@code runId}
     * (files that disappeared). Scoping to the scanned roots avoids deleting entries that
     * belong to roots not included in this run. Returns the number of rows removed.
     */
    int pruneNotSeen(long runId, List<String> rootPrefixes);

    /** Streams every record that has a content hash, for exact-duplicate grouping. */
    void forEachHashed(Consumer<ImageRecord> consumer);

    /** Streams every record that has a perceptual hash, for near-duplicate matching. */
    void forEachWithPerceptualHash(Consumer<ImageRecord> consumer);

    @Override
    void close();
}
