package io.github.alexbosch3000.dupscan.cli.app;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Top-level command. Holds subcommands; running it with no subcommand prints usage. */
@Command(
        name = "dupscan",
        mixinStandardHelpOptions = true,
        version = "dupscan 0.1.0",
        description = "Find duplicate pictures across folders (exact and near-duplicate).",
        subcommands = {ScanCommand.class})
public final class DupscanCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // No subcommand provided: show usage and signal a usage error.
        return 2;
    }
}
