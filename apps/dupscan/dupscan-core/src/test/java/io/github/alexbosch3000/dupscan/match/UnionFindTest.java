package io.github.alexbosch3000.dupscan.match;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnionFindTest {

    @Test
    void unionsAreTransitive() {
        UnionFind uf = new UnionFind(5);
        uf.union(0, 1);
        uf.union(1, 2);

        assertThat(uf.find(0)).isEqualTo(uf.find(2));
        assertThat(uf.find(3)).isNotEqualTo(uf.find(0));
        assertThat(uf.find(4)).isNotEqualTo(uf.find(3));
    }
}
