package io.github.alexbosch3000.dupscan.hash;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

/**
 * Loads a reduced-resolution raster suitable for perceptual hashing without decoding the
 * full image into memory: it uses the reader's source-subsampling so a 60-megapixel photo is
 * decoded to a few hundred pixels per side.
 *
 * <p>This is also the seam for future RAW/HEIC support: formats no recognised reader can
 * handle return {@link Optional#empty()} (the file is still content-hashed and indexed, just
 * excluded from near-duplicate matching). Native preview extraction (libheif/dcraw) can be
 * added here without touching callers.
 */
public final class PreviewExtractor {

    /**
     * @param maxDim approximate target for the larger side; the result may be somewhat
     *               larger because subsampling is integer-stepped.
     */
    public Optional<BufferedImage> loadReduced(Path file, int maxDim) throws IOException {
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
                int largest = Math.max(w, h);
                int sub = Math.max(1, largest / Math.max(1, maxDim));

                ImageReadParam param = reader.getDefaultReadParam();
                if (param.canSetSourceRenderSize()) {
                    // honoured by some readers; ignored otherwise
                }
                param.setSourceSubsampling(sub, sub, 0, 0);
                BufferedImage image = reader.read(0, param);
                return Optional.ofNullable(image);
            } finally {
                reader.dispose();
            }
        } catch (RuntimeException e) {
            // Defensive: a malformed file must never crash the worker.
            throw new IOException("Failed to decode preview for " + file, e);
        }
    }
}
