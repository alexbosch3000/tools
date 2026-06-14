package io.github.alexbosch3000.dupscan.hash;

import io.github.alexbosch3000.dupscan.TestImages;
import io.github.alexbosch3000.dupscan.config.HashAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PerceptualHasherTest {

    private final PerceptualHasher hasher =
            new PerceptualHasher(HashAlgorithm.DHASH, new PreviewExtractor());

    @Test
    void sameImageAcrossFormatsHashesClose(@TempDir Path tmp) throws IOException {
        BufferedImage img = TestImages.gradient(256, 256, true, 0);
        Path jpg = TestImages.write(img, tmp.resolve("a.jpg"), "jpg");
        Path png = TestImages.write(img, tmp.resolve("a.png"), "png");

        long hj = hasher.hash(jpg).orElseThrow();
        long hp = hasher.hash(png).orElseThrow();

        assertThat(PerceptualHasher.hammingDistance(hj, hp)).isLessThanOrEqualTo(8);
    }

    @Test
    void visuallyDifferentImagesHashFar(@TempDir Path tmp) throws IOException {
        Path horizontal = TestImages.write(
                TestImages.gradient(256, 256, true, 0), tmp.resolve("h.jpg"), "jpg");
        Path vertical = TestImages.write(
                TestImages.gradient(256, 256, false, 40), tmp.resolve("v.jpg"), "jpg");

        long hh = hasher.hash(horizontal).orElseThrow();
        long hv = hasher.hash(vertical).orElseThrow();

        assertThat(PerceptualHasher.hammingDistance(hh, hv)).isGreaterThan(13);
    }
}
