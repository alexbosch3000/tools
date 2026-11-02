package io.github.alexbosch3000.dupscan.scan;

import io.github.alexbosch3000.dupscan.pipeline.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Walks the already-normalised roots and emits image candidates. By default symbolic links
 * are ignored entirely (link targets are not regular files when links are not followed),
 * which keeps the walk bounded and prevents scanning a file twice. With
 * {@code followSymlinks} on, the JDK detects directory cycles and a visited-real-path set
 * suppresses duplicate emissions; failures (permissions, loops) are logged and skipped.
 */
public final class FileSystemWorkSource implements WorkSource {

    private static final Logger log = LoggerFactory.getLogger(FileSystemWorkSource.class);

    private final List<Path> roots;
    private final ImageFileFilter filter;
    private final long minSizeBytes;
    private final boolean followSymlinks;

    public FileSystemWorkSource(List<Path> normalizedRoots, ImageFileFilter filter,
                                long minSizeBytes, boolean followSymlinks) {
        this.roots = List.copyOf(normalizedRoots);
        this.filter = filter;
        this.minSizeBytes = minSizeBytes;
        this.followSymlinks = followSymlinks;
    }

    @Override
    public long produce(Consumer<WorkItem> sink) throws IOException {
        long[] count = {0};
        Set<Path> visited = followSymlinks ? new HashSet<>() : null;
        EnumSet<FileVisitOption> options = followSymlinks
                ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
                : EnumSet.noneOf(FileVisitOption.class);

        for (Path root : roots) {
            Files.walkFileTree(root, options, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile() || !filter.test(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (attrs.size() < minSizeBytes) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (visited != null) {
                        Path real;
                        try {
                            real = file.toRealPath();
                        } catch (IOException e) {
                            log.warn("Cannot resolve {}: {}", file, e.toString());
                            return FileVisitResult.CONTINUE;
                        }
                        if (!visited.add(real)) {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    sink.accept(new WorkItem(file, attrs.size(), attrs.lastModifiedTime().toMillis()));
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Skipping unreadable path {}: {}", file, exc.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return count[0];
    }
}
