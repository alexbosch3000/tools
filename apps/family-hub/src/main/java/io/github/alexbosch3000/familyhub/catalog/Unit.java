// Units of measure for items and ingredients, with their conversion factor to the base unit of their dimension.
package io.github.alexbosch3000.familyhub.catalog;

/** Base units are the gram for mass and the millilitre for volume; spoons follow French kitchen usage. */
public enum Unit {
    G(Dimension.MASS, 1),
    KG(Dimension.MASS, 1000),
    ML(Dimension.VOLUME, 1),
    CL(Dimension.VOLUME, 10),
    L(Dimension.VOLUME, 1000),
    TSP(Dimension.VOLUME, 5),
    TBSP(Dimension.VOLUME, 15),
    PIECE(Dimension.PIECE, 1),
    PACK(Dimension.PACK, 1),
    BUNCH(Dimension.BUNCH, 1),
    PINCH(Dimension.PINCH, 1);

    private final Dimension dimension;
    private final double factor;

    Unit(Dimension dimension, double factor) {
        this.dimension = dimension;
        this.factor = factor;
    }

    public Dimension dimension() {
        return dimension;
    }

    /** Amount of the base unit in one of this unit. */
    public double factor() {
        return factor;
    }
}
