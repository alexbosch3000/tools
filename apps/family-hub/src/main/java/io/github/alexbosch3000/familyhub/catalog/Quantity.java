// An amount in a unit, with conversion, scaling and aggregation of amounts sharing a dimension.
package io.github.alexbosch3000.familyhub.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Quantity(double amount, Unit unit) {

    public Quantity {
        if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Invalid amount " + amount);
        }
    }

    public Quantity scaled(double ratio) {
        return new Quantity(amount * ratio, unit);
    }

    /** Converts to another unit of the same dimension, or returns null when the dimensions differ. */
    public Double in(Unit target) {
        if (target.dimension() != unit.dimension()) {
            return null;
        }
        return amount * unit.factor() / target.factor();
    }

    /**
     * Adds up quantities per dimension, in the order dimensions first appear. Amounts keep their unit
     * when they all share it ("3 c. à soupe"); otherwise they are converted to the base unit and shown
     * in the larger unit from 1000 upwards (1500 g becomes 1.5 kg).
     */
    public static List<Quantity> aggregate(List<Quantity> quantities) {
        Map<Dimension, List<Quantity>> byDimension = new LinkedHashMap<>();
        for (Quantity q : quantities) {
            byDimension.computeIfAbsent(q.unit().dimension(), d -> new ArrayList<>()).add(q);
        }
        List<Quantity> result = new ArrayList<>();
        for (List<Quantity> group : byDimension.values()) {
            Unit first = group.getFirst().unit();
            if (group.stream().allMatch(q -> q.unit() == first)) {
                result.add(new Quantity(group.stream().mapToDouble(Quantity::amount).sum(), first));
            } else {
                double base = group.stream().mapToDouble(q -> q.amount() * q.unit().factor()).sum();
                result.add(readable(base, first.dimension()));
            }
        }
        return result;
    }

    private static Quantity readable(double base, Dimension dimension) {
        return switch (dimension) {
            case MASS -> base >= 1000 ? new Quantity(base / 1000, Unit.KG) : new Quantity(base, Unit.G);
            case VOLUME -> base >= 1000 ? new Quantity(base / 1000, Unit.L) : new Quantity(base, Unit.ML);
            case PIECE -> new Quantity(base, Unit.PIECE);
            case PACK -> new Quantity(base, Unit.PACK);
            case BUNCH -> new Quantity(base, Unit.BUNCH);
            case PINCH -> new Quantity(base, Unit.PINCH);
        };
    }
}
