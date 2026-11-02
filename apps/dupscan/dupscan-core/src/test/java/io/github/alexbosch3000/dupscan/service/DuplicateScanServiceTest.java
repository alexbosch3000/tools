package io.github.alexbosch3000.dupscan.service;

import io.github.alexbosch3000.dupscan.TestImages;
import io.github.alexbosch3000.dupscan.config.MatchMode;
import io.github.alexbosch3000.dupscan.config.ScanConfig;
import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.match.MatchType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateScanServiceTest {

    /**
     * Lays out: a.jpg, an exact byte copy in a subfolder, the same image re-encoded as PNG
     * (near duplicate), and an unrelated image. The three related files must form one group;
     * the unrelated one must not appear.
     */
    private Path layoutPhotos(Path tmp) throws IOException {
        Path photos = Files.createDirectories(tmp.resolve("photos"));
        BufferedImage horizontal = TestImages.gradient(256, 256, true, 0);

        Path a = TestImages.write(horizontal, photos.resolve("a.jpg"), "jpg");
        Path sub = Files.createDirectories(photos.resolve("backup"));
        Files.copy(a, sub.resolve("copy_of_a.jpg"));            // exact duplicate
        TestImages.write(horizontal, photos.resolve("a_as.png"), "png"); // near duplicate
        TestImages.write(TestImages.gradient(256, 256, false, 40),
                photos.resolve("different.jpg"), "jpg");        // unrelated
        return photos;
    }

    @Test
    void detectsExactAndNearDuplicatesAsOneGroup(@TempDir Path tmp) throws Exception {
        Path photos = layoutPhotos(tmp);
        ScanConfig config = ScanConfig.builder()
                .roots(List.of(photos))
                .indexPath(tmp.resolve("index.db"))
                .threshold(0.2)
                .threads(2)
                .build();

        ScanResult result = new DuplicateScanService().run(config, ProgressListener.noop());

        assertThat(result.totalGroups()).isEqualTo(1);
        DuplicateGroup group = result.groups().get(0);
        assertThat(group.size()).isEqualTo(3);
        assertThat(group.matchType()).isEqualTo(MatchType.NEAR);
        assertThat(group.members().stream().map(ImageRecord::fileName))
                .containsExactlyInAnyOrder("a.jpg", "copy_of_a.jpg", "a_as.png");
    }

    @Test
    void exactModeFindsOnlyByteIdenticalFiles(@TempDir Path tmp) throws Exception {
        Path photos = layoutPhotos(tmp);
        ScanConfig config = ScanConfig.builder()
                .roots(List.of(photos))
                .indexPath(tmp.resolve("index.db"))
                .mode(MatchMode.EXACT)
                .threads(2)
                .build();

        ScanResult result = new DuplicateScanService().run(config, ProgressListener.noop());

        assertThat(result.totalGroups()).isEqualTo(1);
        DuplicateGroup group = result.groups().get(0);
        assertThat(group.matchType()).isEqualTo(MatchType.EXACT);
        assertThat(group.members().stream().map(ImageRecord::fileName))
                .containsExactlyInAnyOrder("a.jpg", "copy_of_a.jpg");
    }

    @Test
    void secondRunReusesIndexInsteadOfRehashing(@TempDir Path tmp) throws Exception {
        Path photos = layoutPhotos(tmp);
        ScanConfig config = ScanConfig.builder()
                .roots(List.of(photos))
                .indexPath(tmp.resolve("index.db"))
                .threads(2)
                .build();
        DuplicateScanService service = new DuplicateScanService();

        ScanResult first = service.run(config, ProgressListener.noop());
        assertThat(first.created()).isEqualTo(4);
        assertThat(first.reused()).isZero();

        ScanResult second = service.run(config, ProgressListener.noop());
        assertThat(second.created()).isZero();
        assertThat(second.reused()).isEqualTo(4);
        assertThat(second.totalGroups()).isEqualTo(1);
    }
}
