// Group of an expense category in the 50/30/20 rule, with the share of income it targets.
package io.github.alexbosch3000.familyhub.accounting;

public enum BudgetGroup {
    NEEDS(50),
    WANTS(30),
    SAVINGS(20);

    private final int targetPercent;

    BudgetGroup(int targetPercent) {
        this.targetPercent = targetPercent;
    }

    public int targetPercent() {
        return targetPercent;
    }
}
