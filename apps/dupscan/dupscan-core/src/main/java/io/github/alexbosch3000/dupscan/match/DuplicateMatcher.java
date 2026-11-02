package io.github.alexbosch3000.dupscan.match;

import io.github.alexbosch3000.dupscan.config.MatchMode;
import io.github.alexbosch3000.dupscan.config.ScanConfig;
import io.github.alexbosch3000.dupscan.hash.PerceptualHasher;
import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.index.IndexStore;
import io.github.alexbosch3000.dupscan.original.OriginalSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds duplicate groups from the index. Exact duplicates are unioned by shared SHA-256;
 * near duplicates are unioned via a {@link BkTree} within the configured Hamming tolerance.
 * Both feed one {@link UnionFind}, so in {@code EXACT_PERCEPTUAL} mode a cluster transparently
 * contains byte-identical and visually-identical members. Groups are returned sorted by
 * reclaimable space, descending.
 */
public final class DuplicateMatcher {

    private final ScanConfig config;
    private final OriginalSelector selector;

    public DuplicateMatcher(ScanConfig config, OriginalSelector selector) {
        this.config = config;
        this.selector = selector;
    }

    public List<DuplicateGroup> findGroups(IndexStore index) {
        List<ImageRecord> recs = new ArrayList<>();
        if (config.mode() == MatchMode.PERCEPTUAL) {
            index.forEachWithPerceptualHash(recs::add);
        } else {
            index.forEachHashed(recs::add); // ordered by content_sha256
        }

        int n = recs.size();
        UnionFind uf = new UnionFind(n);

        if (config.wantsExact() && config.mode() != MatchMode.PERCEPTUAL) {
            for (int i = 1; i < n; i++) {
                String prev = recs.get(i - 1).contentSha256();
                String cur = recs.get(i).contentSha256();
                if (cur != null && cur.equals(prev)) {
                    uf.union(i - 1, i);
                }
            }
        }

        if (config.wantsPerceptual()) {
            BkTree tree = new BkTree();
            int maxDist = config.maxHammingDistance();
            for (int i = 0; i < n; i++) {
                ImageRecord r = recs.get(i);
                if (!r.hasPerceptualHash()) {
                    continue;
                }
                long h = r.perceptualHash();
                final int self = i;
                tree.query(h, maxDist, other -> uf.union(self, other));
                tree.add(h, i);
            }
        }

        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(i);
        }

        List<DuplicateGroup> groups = new ArrayList<>();
        int id = 1;
        for (List<Integer> ids : clusters.values()) {
            if (ids.size() < 2) {
                continue;
            }
            List<ImageRecord> members = new ArrayList<>(ids.size());
            for (int idx : ids) {
                members.add(recs.get(idx));
            }
            MatchType type = allSameSha(members) ? MatchType.EXACT : MatchType.NEAR;
            ImageRecord original = selector.select(members);

            Map<String, Integer> hamming = new LinkedHashMap<>();
            for (ImageRecord m : members) {
                hamming.put(m.canonicalPath(), distance(original, m));
            }
            List<ImageRecord> duplicates = members.stream()
                    .filter(m -> !m.canonicalPath().equals(original.canonicalPath()))
                    .toList();

            groups.add(new DuplicateGroup(id++, type, original, duplicates, hamming));
        }

        groups.sort((a, b) -> Long.compare(b.reclaimableBytes(), a.reclaimableBytes()));
        // Re-number after sorting so group ids reflect report order.
        List<DuplicateGroup> renumbered = new ArrayList<>(groups.size());
        int gid = 1;
        for (DuplicateGroup g : groups) {
            renumbered.add(new DuplicateGroup(gid++, g.matchType(), g.original(),
                    g.duplicates(), g.hammingByPath()));
        }
        return renumbered;
    }

    private static boolean allSameSha(List<ImageRecord> members) {
        String sha = members.get(0).contentSha256();
        if (sha == null) {
            return false;
        }
        return members.stream().allMatch(m -> sha.equals(m.contentSha256()));
    }

    private static int distance(ImageRecord original, ImageRecord member) {
        if (original.hasPerceptualHash() && member.hasPerceptualHash()
                && !Objects.equals(original.canonicalPath(), member.canonicalPath())) {
            return PerceptualHasher.hammingDistance(original.perceptualHash(), member.perceptualHash());
        }
        return 0;
    }
}
