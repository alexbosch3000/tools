package io.github.alexbosch3000.dupscan.cli.app;

import io.github.alexbosch3000.dupscan.cli.report.StdoutTableReporter;
import io.github.alexbosch3000.dupscan.config.HashAlgorithm;
import io.github.alexbosch3000.dupscan.config.MatchMode;
import io.github.alexbosch3000.dupscan.config.OriginalRule;
import io.github.alexbosch3000.dupscan.config.ScanConfig;
import io.github.alexbosch3000.dupscan.report.CsvReporter;
import io.github.alexbosch3000.dupscan.report.ExcelReporter;
import io.github.alexbosch3000.dupscan.service.DuplicateScanService;
import io.github.alexbosch3000.dupscan.service.ScanResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * The {@code scan} subcommand: scans folders, detects exact and near-duplicate pictures and
 * reports them to the console, Excel and/or CSV. Exit codes: 0 = no duplicates, 1 =
 * duplicates found, 2 = usage error, 3 = runtime error.
 */
@Command(
        name = "scan",
        description = "Scan folders and report duplicate pictures.",
        mixinStandardHelpOptions = true,
        sortOptions = false)
public final class ScanCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"-p", "--path"}, required = true, paramLabel = "DIR",
            description = "Folder to scan (repeatable). Nested folders are de-duplicated.")
    List<Path> paths = new ArrayList<>();

    @Option(names = "--include", paramLabel = "GLOB",
            description = "Only include paths matching this glob (repeatable).")
    List<String> include = new ArrayList<>();

    @Option(names = "--exclude", paramLabel = "GLOB",
            description = "Exclude paths matching this glob (repeatable).")
    List<String> exclude = new ArrayList<>();

    @Option(names = "--formats", split = ",", paramLabel = "EXT",
            description = "Comma-separated extensions to scan (default: common + heic/heif + raw).")
    Set<String> formats;

    @Option(names = "--min-size", defaultValue = "0", paramLabel = "BYTES",
            description = "Ignore files smaller than this many bytes (default: 0).")
    long minSize;

    @Option(names = "--follow-symlinks", description = "Follow symbolic links (off by default).")
    boolean followSymlinks;

    @Option(names = {"-m", "--mode"}, defaultValue = "exact+perceptual", paramLabel = "MODE",
            description = "exact | perceptual | exact+perceptual (default: ${DEFAULT-VALUE}).")
    String mode;

    @Option(names = {"-t", "--threshold"}, defaultValue = "0.15", paramLabel = "0.0-1.0",
            description = "Perceptual tolerance as normalized Hamming distance (default: ${DEFAULT-VALUE}).")
    double threshold;

    @Option(names = "--hash", defaultValue = "dhash", paramLabel = "ALGO",
            description = "Perceptual algorithm: dhash | phash (default: ${DEFAULT-VALUE}).")
    String hash;

    @Option(names = "--original-rule", defaultValue = "highest-quality", paramLabel = "RULE",
            description = "highest-quality | oldest | shortest-path (default: ${DEFAULT-VALUE}).")
    String originalRule;

    @Option(names = "--index", paramLabel = "FILE",
            description = "SQLite index for incremental re-scans (default: ~/.dupscan/index.db).")
    Path index;

    @Option(names = "--rehash", description = "Ignore the cache and re-hash every file.")
    boolean rehash;

    @Option(names = "--prune", description = "Remove index rows for files that no longer exist.")
    boolean prune;

    @Option(names = {"-r", "--report"}, paramLabel = "FILE.xlsx",
            description = "Write an Excel report.")
    Path reportXlsx;

    @Option(names = "--csv", paramLabel = "FILE.csv",
            description = "Write a CSV report (recommended at very large scale).")
    Path reportCsv;

    @Option(names = "--top", defaultValue = "50", paramLabel = "N",
            description = "Show at most N groups in the console table (default: ${DEFAULT-VALUE}).")
    int top;

    @Option(names = "--no-table", description = "Suppress the console table; print summary only.")
    boolean noTable;

    @Option(names = "--threads", defaultValue = "0", paramLabel = "N",
            description = "Worker threads (default: number of CPU cores).")
    int threads;

    @Option(names = "--max-pixels", defaultValue = "268435456", paramLabel = "N",
            description = "Reject images larger than this many pixels (decode-bomb guard).")
    long maxPixels;

    @Option(names = "--max-bytes", defaultValue = "2147483648", paramLabel = "N",
            description = "Reject files larger than this many bytes (decode-bomb guard).")
    long maxBytes;

    @Override
    public Integer call() throws Exception {
        ScanConfig config = buildConfig();

        DuplicateScanService service = new DuplicateScanService();
        ScanResult result = service.run(config, new ConsoleProgressListener(System.err));

        new StdoutTableReporter(System.out, config.topGroups(), config.showTable()).write(result);

        if (config.reportXlsx() != null) {
            new ExcelReporter(config.reportXlsx()).write(result);
            System.out.println("Wrote Excel report: " + config.reportXlsx().toAbsolutePath());
        }
        if (config.reportCsv() != null) {
            new CsvReporter(config.reportCsv()).write(result);
            System.out.println("Wrote CSV report:   " + config.reportCsv().toAbsolutePath());
        }

        return result.groups().isEmpty() ? 0 : 1;
    }

    private ScanConfig buildConfig() {
        ScanConfig.Builder b = ScanConfig.builder()
                .roots(paths)
                .includeGlobs(include)
                .excludeGlobs(exclude)
                .minSizeBytes(minSize)
                .followSymlinks(followSymlinks)
                .mode(parseMode(mode))
                .threshold(threshold)
                .hashAlgorithm(parseHash(hash))
                .originalRule(parseRule(originalRule))
                .rehash(rehash)
                .prune(prune)
                .reportXlsx(reportXlsx)
                .reportCsv(reportCsv)
                .topGroups(top)
                .showTable(!noTable)
                .maxPixels(maxPixels)
                .maxBytes(maxBytes);

        if (formats != null && !formats.isEmpty()) {
            b.formats(formats.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet()));
        }
        if (index != null) {
            b.indexPath(index);
        }
        if (threads > 0) {
            b.threads(threads);
        }
        try {
            return b.build();
        } catch (IllegalArgumentException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        }
    }

    private MatchMode parseMode(String v) {
        return switch (v.toLowerCase(Locale.ROOT)) {
            case "exact" -> MatchMode.EXACT;
            case "perceptual" -> MatchMode.PERCEPTUAL;
            case "exact+perceptual", "both" -> MatchMode.EXACT_PERCEPTUAL;
            default -> throw new CommandLine.ParameterException(spec.commandLine(),
                    "Invalid --mode '" + v + "' (expected exact | perceptual | exact+perceptual)");
        };
    }

    private HashAlgorithm parseHash(String v) {
        return switch (v.toLowerCase(Locale.ROOT)) {
            case "dhash" -> HashAlgorithm.DHASH;
            case "phash" -> HashAlgorithm.PHASH;
            default -> throw new CommandLine.ParameterException(spec.commandLine(),
                    "Invalid --hash '" + v + "' (expected dhash | phash)");
        };
    }

    private OriginalRule parseRule(String v) {
        return switch (v.toLowerCase(Locale.ROOT)) {
            case "highest-quality" -> OriginalRule.HIGHEST_QUALITY;
            case "oldest" -> OriginalRule.OLDEST;
            case "shortest-path" -> OriginalRule.SHORTEST_PATH;
            default -> throw new CommandLine.ParameterException(spec.commandLine(),
                    "Invalid --original-rule '" + v + "' (expected highest-quality | oldest | shortest-path)");
        };
    }
}
