// What will actually be bought for a shopping row, when it differs from what its lines add up to.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import org.jspecify.annotations.Nullable;

/**
 * @param quantity   the quantity to buy
 * @param priceCents the estimated price of that quantity, null to derive it from the catalog price
 */
public record Purchase(Quantity quantity, @Nullable Long priceCents, @Nullable String note) {
}
