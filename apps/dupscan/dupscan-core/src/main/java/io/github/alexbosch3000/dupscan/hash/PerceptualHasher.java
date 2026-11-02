package io.github.alexbosch3000.dupscan.hash;

import io.github.alexbosch3000.dupscan.config.HashAlgorithm;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Computes a 64-bit perceptual hash. Two algorithms are supported, both compared later by
 * Hamming distance:
 * <ul>
 *   <li><b>dHash</b> (default): gradient of a 9x8 grayscale grid; fast and robust to
 *       scaling/compression.</li>
 *   <li><b>pHash</b>: low-frequency DCT coefficients of a 32x32 grayscale grid; more robust
 *       to small edits.</li>
 * </ul>
 */
public final class PerceptualHasher {

    private final HashAlgorithm algorithm;
    private final PreviewExtractor extractor;

    public PerceptualHasher(HashAlgorithm algorithm, PreviewExtractor extractor) {
        this.algorithm = algorithm;
        this.extractor = extractor;
    }

    /** Empty when the file cannot be decoded (e.g. RAW/HEIC with no supported reader). */
    public Optional<Long> hash(Path file) throws IOException {
        int target = algorithm == HashAlgorithm.PHASH ? 64 : 32;
        return extractor.loadReduced(file, target).map(this::hashImage);
    }

    long hashImage(BufferedImage image) {
        return algorithm == HashAlgorithm.PHASH ? pHash(image) : dHash(image);
    }

    private long dHash(BufferedImage image) {
        int[][] g = grayGrid(image, 9, 8);
        long hash = 0L;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                hash = (hash << 1) | (g[y][x] < g[y][x + 1] ? 1L : 0L);
            }
        }
        return hash;
    }

    private long pHash(BufferedImage image) {
        final int n = 32;
        int[][] g = grayGrid(image, n, n);
        double[][] dct = dct2d(g, n);

        double[] low = new double[64];
        int k = 0;
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                low[k++] = dct[u][v];
            }
        }
        // Median over the AC terms (exclude the DC coefficient at index 0).
        double[] ac = Arrays.copyOfRange(low, 1, 64);
        Arrays.sort(ac);
        double median = (ac[ac.length / 2 - 1] + ac[ac.length / 2]) / 2.0;

        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            hash = (hash << 1) | (low[i] > median ? 1L : 0L);
        }
        return hash;
    }

    private static int[][] grayGrid(BufferedImage src, int w, int h) {
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = gray.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();

        Raster raster = gray.getRaster();
        int[][] out = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y][x] = raster.getSample(x, y, 0);
            }
        }
        return out;
    }

    private static double[][] dct2d(int[][] f, int n) {
        double[][] cos = new double[n][n];
        for (int x = 0; x < n; x++) {
            for (int u = 0; u < n; u++) {
                cos[x][u] = Math.cos(((2 * x + 1) * u * Math.PI) / (2.0 * n));
            }
        }
        double[][] result = new double[n][n];
        for (int u = 0; u < n; u++) {
            double cu = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
            for (int v = 0; v < n; v++) {
                double cv = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                double sum = 0.0;
                for (int x = 0; x < n; x++) {
                    for (int y = 0; y < n; y++) {
                        sum += f[x][y] * cos[x][u] * cos[y][v];
                    }
                }
                result[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return result;
    }

    /** Number of differing bits between two 64-bit perceptual hashes. */
    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }
}
