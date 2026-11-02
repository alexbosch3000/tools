package io.github.alexbosch3000.dupscan.cli.app;

import io.github.alexbosch3000.dupscan.service.ProgressListener;

import java.io.PrintStream;

/** Prints phase transitions and periodic hashing progress to a stream (stderr by default). */
final class ConsoleProgressListener implements ProgressListener {

    private final PrintStream out;

    ConsoleProgressListener(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onPhase(String phase) {
        out.println("[dupscan] " + phase + " ...");
    }

    @Override
    public void onHashProgress(long processed, long total) {
        if (total > 0) {
            out.printf("\r[dupscan] processed %,d / %,d", processed, total);
        } else {
            out.printf("\r[dupscan] processed %,d", processed);
        }
        out.flush();
    }
}
