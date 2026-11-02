package io.github.alexbosch3000.dupscan.hash;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

/**
 * Reads only an image's header to learn its dimensions and reject decode bombs before any
 * pixels are decoded. Dimensions are also reused as metadata, avoiding a second header read.
 */
public final class DecodeGuard {

    private final long maxPixels;
    private final long maxBytes;

    public DecodeGuard(long maxPixels, long maxBytes) {
        this.maxPixels = maxPixels;
        this.maxBytes = maxBytes;
    }

    public record Dimensions(int width, int height, String formatName) {
        public long pixels() {
            return (long) Math.max(0, width) * Math.max(0, height);
        }
    }

    public boolean fileWithinByteLimit(long sizeBytes) {
        return sizeBytes <= maxBytes;
    }

    /** Header-only dimension read. Empty if no reader recognises the file. */
    public Optional<Dimensions> readDimensions(Path file) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
            if (iis == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                return Optional.of(new Dimensions(w, h, reader.getFormatName()));
            } finally {
                reader.dispose();
            }
        }
    }

    public boolean pixelsWithinLimit(long pixels) {
        return pixels > 0 && pixels <= maxPixels;
    }
}
