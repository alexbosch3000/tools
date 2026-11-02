package io.github.alexbosch3000.dupscan.match;

import io.github.alexbosch3000.dupscan.index.ImageRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A cluster of duplicate images: one chosen {@code original} plus its {@code duplicates}.
 * {@code hammingByPath} holds each member's perceptual distance to the original (0 for
 * exact groups). Reclaimable space is the sum of the duplicates' sizes.
 */
public record DuplicateGroup(
        int id,
        MatchType matchType,
        ImageRecord original,
        List<ImageRecord> duplicates,
        Map<String, Integer> hammingByPath) {

    public DuplicateGroup {
        duplicates = List.copyOf(duplicates);
        hammingByPath = Map.copyOf(hammingByPath);
    }

    /** Original first, then duplicates. */
    public List<ImageRecord> members() {
        List<ImageRecord> all = new ArrayList<>(duplicates.size() + 1);
        all.add(original);
        all.addAll(duplicates);
        return all;
    }

    public int size() {
        return duplicates.size() + 1;
    }

    public long reclaimableBytes() {
        long sum = 0;
        for (ImageRecord d : duplicates) {
            sum += d.sizeBytes();
        }
        return sum;
    }

    public int hammingOf(ImageRecord member) {
        return hammingByPath.getOrDefault(member.canonicalPath(), 0);
    }
}
