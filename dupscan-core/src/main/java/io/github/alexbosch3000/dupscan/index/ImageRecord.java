package io.github.alexbosch3000.dupscan.index;

import java.nio.file.Path;

/**
 * One indexed image. Mirrors a row in the persistent index. {@code perceptualHash} and
 * {@code exifMillis} are nullable (e.g. undecodable RAW without a preview, or missing EXIF).
 */
public record ImageRecord(
        String canonicalPath,
        long sizeBytes,
        long modifiedMillis,
        String contentSha256,
        Long perceptualHash,
        int width,
        int height,
        String format,
        Long exifMillis) {

    public boolean hasPerceptualHash() {
        return perceptualHash != null;
    }

    public long pixels() {
        return (long) Math.max(0, width) * Math.max(0, height);
    }

    /** Best-known capture/creation time: EXIF when present, otherwise file last-modified. */
    public long effectiveDateMillis() {
        return exifMillis != null ? exifMillis : modifiedMillis;
    }

    public String fileName() {
        return Path.of(canonicalPath).getFileName().toString();
    }
}
