package io.github.alexbosch3000.dupscan.service;

import io.github.alexbosch3000.dupscan.match.DuplicateGroup;

import java.util.List;

/** Outcome of a scan: the duplicate groups plus run statistics. */
public record ScanResult(
        List<DuplicateGroup> groups,
        long discovered,
        long created,
        long reused,
        long skipped,
        long pruned,
        long elapsedMillis) {

    public ScanResult {
        groups = List.copyOf(groups);
    }

    public int totalGroups() {
        return groups.size();
    }

    public long totalDuplicateFiles() {
        return groups.stream().mapToLong(g -> g.duplicates().size()).sum();
    }

    public long reclaimableBytes() {
        return groups.stream().mapToLong(DuplicateGroup::reclaimableBytes).sum();
    }
}
