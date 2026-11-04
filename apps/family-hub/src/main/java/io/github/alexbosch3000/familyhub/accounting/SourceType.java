// Where a transaction comes from; with the source reference it prevents recording the same thing twice.
package io.github.alexbosch3000.familyhub.accounting;

public enum SourceType {
    MANUAL,
    PLANNING,
    SHOPPING,
    RECURRING
}
