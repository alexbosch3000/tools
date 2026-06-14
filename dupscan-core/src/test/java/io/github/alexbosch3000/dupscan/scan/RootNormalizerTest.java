package io.github.alexbosch3000.dupscan.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RootNormalizerTest {

    @Test
    void collapsesNestedRoots(@TempDir Path tmp) throws IOException {
        Path parent = Files.createDirectories(tmp.resolve("photos"));
        Path child = Files.createDirectories(parent.resolve("2024"));

        List<Path> result = RootNormalizer.normalize(List.of(child, parent));

        assertThat(result).containsExactly(parent.toRealPath());
    }

    @Test
    void deduplicatesIdenticalRoots(@TempDir Path tmp) throws IOException {
        Path dir = Files.createDirectories(tmp.resolve("a"));

        List<Path> result = RootNormalizer.normalize(List.of(dir, dir));

        assertThat(result).hasSize(1);
    }

    @Test
    void rejectsMissingRoot(@TempDir Path tmp) {
        assertThatThrownBy(() -> RootNormalizer.normalize(List.of(tmp.resolve("nope"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
