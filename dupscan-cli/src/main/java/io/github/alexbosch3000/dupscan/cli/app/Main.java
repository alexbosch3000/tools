package io.github.alexbosch3000.dupscan.cli.app;

import picocli.CommandLine;

/** Entry point for the runnable jar. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new DupscanCommand());
        if (args.length == 0) {
            cmd.usage(System.out);
            System.exit(2);
        }
        System.exit(cmd.execute(args));
    }
}
