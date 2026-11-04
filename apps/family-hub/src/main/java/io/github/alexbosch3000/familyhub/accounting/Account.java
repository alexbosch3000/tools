// A money account and its balance: opening balance plus approved income minus approved expenses.
package io.github.alexbosch3000.familyhub.accounting;

public record Account(long id, String name, AccountKind kind, long openingBalanceCents, long balanceCents) {
}
