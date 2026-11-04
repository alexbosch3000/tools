// A ledger transaction. Amounts are positive; the direction comes from the category.
package io.github.alexbosch3000.familyhub.accounting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

public record Transaction(long id, LocalDate day, String label, long amountCents, Direction direction,
        String categoryCode, String categoryName, @Nullable Long accountId, TransactionStatus status,
        SourceType sourceType, @Nullable String sourceRef, @Nullable String supplier, @Nullable Long createdBy,
        @Nullable Long approvedBy, @Nullable LocalDateTime approvedAt) {
}
