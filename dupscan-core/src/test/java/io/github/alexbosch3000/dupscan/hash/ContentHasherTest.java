package io.github.alexbosch3000.dupscan.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHasherTest {

    private final ContentHasher hasher = new ContentHasher();

    @Test
    void identicalContentDifferentNameAndDateGivesSameHash(@TempDir Path tmp) throws IOException {
        byte[] bytes = "the same pixels".getBytes();
        Path a = Files.write(tmp.resolve("a.jpg"), bytes);
        Path b = Files.write(tmp.resolve("renamed-copy.jpeg"), bytes);
        Files.setLastModifiedTime(b, FileTime.fromMillis(0));

        assertThat(hasher.sha256(a)).isEqualTo(hasher.sha256(b));
    }

    @Test
    void differentContentGivesDifferentHash(@TempDir Path tmp) throws IOException {
        Path a = Files.write(tmp.resolve("a"), new byte[]{1, 2, 3});
        Path b = Files.write(tmp.resolve("b"), new byte[]{1, 2, 4});

        assertThat(hasher.sha256(a)).isNotEqualTo(hasher.sha256(b));
    }
}
