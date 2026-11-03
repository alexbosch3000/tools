// A ledger transaction. Amounts are positive; the direction comes from the category.
package io.github.alexbosch3000.familyhub.accounting;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Transaction(long id, LocalDate day, String label, long amountCents, Direction direction,
        String categoryCode, String categoryName, Long accountId, TransactionStatus status, SourceType sourceType,
        String sourceRef, String supplier, Long createdBy, Long approvedBy, LocalDateTime approvedAt) {
}
