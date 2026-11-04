// Lifecycle of a transaction: forecast, awaiting approval, then approved by a parent.
package io.github.alexbosch3000.familyhub.accounting;

public enum TransactionStatus {
    PLANNED,
    PENDING,
    APPROVED
}
