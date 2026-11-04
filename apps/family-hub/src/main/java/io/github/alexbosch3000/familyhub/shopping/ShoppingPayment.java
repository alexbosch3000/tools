// Amount paid in one kind of shop during a shopping trip; each becomes an expense in its own category.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.StoreType;

public record ShoppingPayment(StoreType storeType, long amountCents) {
}
