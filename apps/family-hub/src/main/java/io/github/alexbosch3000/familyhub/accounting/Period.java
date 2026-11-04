// How often a recurring transaction falls due, in months.
package io.github.alexbosch3000.familyhub.accounting;

public enum Period {
    MONTHLY(1),
    BIMONTHLY(2),
    QUARTERLY(3),
    SEMIANNUAL(6),
    YEARLY(12);

    private final int months;

    Period(int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }
}
