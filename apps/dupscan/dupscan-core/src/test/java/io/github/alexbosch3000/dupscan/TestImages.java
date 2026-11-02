package io.github.alexbosch3000.dupscan;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Helpers to synthesise deterministic test images so no binary fixtures are committed. */
public final class TestImages {

    private TestImages() {
    }

    /**
     * A smooth gradient image. Smoothness keeps the perceptual hash stable across JPEG/PNG
     * re-encoding; the {@code horizontal} flag produces visually distinct images.
     */
    public static BufferedImage gradient(int w, int h, boolean horizontal, int tint) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Pure grayscale ramp along one axis only, so a horizontal and a vertical
                // gradient differ strongly under a gradient-based perceptual hash.
                int v = clamp((horizontal ? (x * 255 / (w - 1)) : (y * 255 / (h - 1))) + tint);
                img.setRGB(x, y, new Color(v, v, v).getRGB());
            }
        }
        return img;
    }

    public static Path write(BufferedImage img, Path target, String format) throws IOException {
        if (!ImageIO.write(img, format, target.toFile())) {
            throw new IOException("No ImageIO writer for format: " + format);
        }
        return target;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
