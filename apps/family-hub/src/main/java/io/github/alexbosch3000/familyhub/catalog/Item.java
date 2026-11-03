// A purchasable item of the catalog, with where to find it and an estimated price.
package io.github.alexbosch3000.familyhub.catalog;

/**
 * @param unit           default unit, in which the price is expressed
 * @param unitPriceCents estimated price of one {@code unit}, null when unknown
 */
public record Item(long id, String name, ItemKind kind, StoreType storeType, Aisle aisle, Unit unit,
        Long unitPriceCents) {

    /** Estimated cost of a quantity, null when the price is unknown or the units cannot be converted. */
    public Long estimateCents(Quantity quantity) {
        if (unitPriceCents == null) {
            return null;
        }
        Double inDefaultUnit = quantity.in(unit);
        return inDefaultUnit == null ? null : Math.round(inDefaultUnit * unitPriceCents);
    }
}
