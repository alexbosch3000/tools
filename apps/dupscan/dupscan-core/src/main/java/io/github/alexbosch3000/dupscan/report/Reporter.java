package io.github.alexbosch3000.dupscan.report;

import io.github.alexbosch3000.dupscan.service.ScanResult;

import java.io.IOException;

/** Renders a {@link ScanResult} to some destination (file, stream, console). */
public interface Reporter {

    void write(ScanResult result) throws IOException;
}
