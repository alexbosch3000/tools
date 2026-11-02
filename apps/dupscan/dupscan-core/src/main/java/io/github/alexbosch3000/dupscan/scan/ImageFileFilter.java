package io.github.alexbosch3000.dupscan.scan;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Cheap, path-based pre-filter applied during the walk: keeps regular files whose extension
 * is a recognised image format and that satisfy the include/exclude globs. Content (magic
 * byte) verification happens later in the pipeline, where the bytes are read anyway.
 */
public final class ImageFileFilter implements Predicate<Path> {

    private final Set<String> formats;
    private final List<PathMatcher> includes;
    private final List<PathMatcher> excludes;

    public ImageFileFilter(Set<String> formats, List<String> includeGlobs, List<String> excludeGlobs) {
        this.formats = formats;
        this.includes = compile(includeGlobs);
        this.excludes = compile(excludeGlobs);
    }

    private static List<PathMatcher> compile(List<String> globs) {
        return globs.stream()
                .map(g -> FileSystems.getDefault().getPathMatcher("glob:" + g))
                .toList();
    }

    @Override
    public boolean test(Path path) {
        if (!hasImageExtension(path)) {
            return false;
        }
        if (!excludes.isEmpty() && excludes.stream().anyMatch(m -> m.matches(path))) {
            return false;
        }
        return includes.isEmpty() || includes.stream().anyMatch(m -> m.matches(path));
    }

    private boolean hasImageExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        return formats.contains(name.substring(dot + 1).toLowerCase());
    }

    public static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return (dot < 0 || dot == name.length() - 1) ? "" : name.substring(dot + 1).toLowerCase();
    }
}
