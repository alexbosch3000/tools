package io.github.alexbosch3000.dupscan.pipeline;

import java.nio.file.Path;

/**
 * A single candidate file to process, carrying the cheap attributes already gathered during
 * the walk so the pipeline can decide whether the index entry is still current.
 */
public record WorkItem(Path path, long sizeBytes, long modifiedMillis) {
}
