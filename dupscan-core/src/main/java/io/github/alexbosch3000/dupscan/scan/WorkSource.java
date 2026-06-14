package io.github.alexbosch3000.dupscan.scan;

import io.github.alexbosch3000.dupscan.pipeline.WorkItem;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Produces the candidate files to process. v1 walks the local filesystem; this is the seam
 * a future backend can replace with a distributed queue (coordinator/worker sharding)
 * without touching the rest of the engine.
 */
public interface WorkSource {

    /**
     * Emit every candidate exactly once to {@code sink}. Blocks until the source is
     * exhausted. Implementations must guarantee no path is emitted twice.
     *
     * @return the number of candidates emitted
     */
    long produce(Consumer<WorkItem> sink) throws IOException;
}
