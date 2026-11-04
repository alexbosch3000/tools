// Domain event: a shopping trip is over and what was paid is known. Accounting records it, planning closes it.
package io.github.alexbosch3000.familyhub.shopping;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @param accountId account the purchases were paid from, null when not specified
 * @param payments  amounts per kind of shop: groceries, clothes and DIY end up in different categories
 */
public record ShoppingListCompleted(long listId, String name, LocalDate paidOn, @Nullable Long accountId,
        List<ShoppingPayment> payments) {
}
