package io.github.alexbosch3000.dupscan.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * BK-tree over 64-bit perceptual hashes using Hamming distance, mapping each hash to one or
 * more integer ids (row indices). Enables near-neighbour queries within a bit threshold in
 * far less than the O(n^2) of all-pairs comparison, which matters at millions of images.
 */
public final class BkTree {

    private static final class Node {
        final long key;
        final List<Integer> ids = new ArrayList<>(1);
        final Map<Integer, Node> children = new HashMap<>();

        Node(long key) {
            this.key = key;
        }
    }

    private Node root;

    public void add(long key, int id) {
        if (root == null) {
            root = new Node(key);
            root.ids.add(id);
            return;
        }
        Node node = root;
        while (true) {
            int d = Long.bitCount(node.key ^ key);
            if (d == 0) {
                node.ids.add(id);
                return;
            }
            Node child = node.children.get(d);
            if (child == null) {
                Node created = new Node(key);
                created.ids.add(id);
                node.children.put(d, created);
                return;
            }
            node = child;
        }
    }

    /** Invokes {@code consumer} for every stored id whose hash is within {@code maxDistance}. */
    public void query(long key, int maxDistance, IntConsumer consumer) {
        if (root != null) {
            queryNode(root, key, maxDistance, consumer);
        }
    }

    private void queryNode(Node node, long key, int maxDistance, IntConsumer consumer) {
        int d = Long.bitCount(node.key ^ key);
        if (d <= maxDistance) {
            for (int id : node.ids) {
                consumer.accept(id);
            }
        }
        int lo = d - maxDistance;
        int hi = d + maxDistance;
        for (Map.Entry<Integer, Node> e : node.children.entrySet()) {
            int edge = e.getKey();
            if (edge >= lo && edge <= hi) {
                queryNode(e.getValue(), key, maxDistance, consumer);
            }
        }
    }
}
