// Domain event: a shopping trip is over and its amount is known. Accounting records it, planning closes it.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.StoreType;
import java.time.LocalDate;

/**
 * @param accountId      account the purchase was paid from, null when not specified
 * @param mainStoreType  store type of most of the list's items, a hint for categorising the expense
 */
public record ShoppingListCompleted(long listId, String name, LocalDate paidOn, long amountCents, Long accountId,
        StoreType mainStoreType) {
}
