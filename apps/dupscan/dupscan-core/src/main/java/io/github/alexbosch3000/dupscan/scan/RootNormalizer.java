package io.github.alexbosch3000.dupscan.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves the user's roots to canonical real paths and drops any root nested inside
 * another, so the same directory tree is never scanned twice.
 */
public final class RootNormalizer {

    private RootNormalizer() {
    }

    public static List<Path> normalize(List<Path> roots) {
        List<Path> real = new ArrayList<>();
        for (Path root : roots) {
            Path resolved;
            try {
                resolved = root.toRealPath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Root does not exist or is not accessible: " + root, e);
            }
            if (!Files.isDirectory(resolved)) {
                throw new IllegalArgumentException("Root is not a directory: " + root);
            }
            real.add(resolved);
        }

        // Shorter paths first so ancestors are kept and descendants discarded.
        real.sort(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString));

        List<Path> kept = new ArrayList<>();
        for (Path candidate : real) {
            boolean nested = kept.stream().anyMatch(candidate::startsWith);
            if (!nested && !kept.contains(candidate)) {
                kept.add(candidate);
            }
        }
        return List.copyOf(kept);
    }
}
