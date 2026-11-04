// Budget metrics: transactions approved, and transactions waiting for a parent's approval.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.shared.Telemetry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class AccountingMetrics {

    private final Counter approved;

    AccountingMetrics(AccountingRepository repository, Clock clock) {
        Gauge.builder(Telemetry.PREFIX + "accounting.approvals.pending",
                        () -> repository.findToApprove(LocalDate.now(clock)).size())
                .description("Transactions waiting for a parent's approval").register(Telemetry.registry());
        approved = Counter.builder(Telemetry.PREFIX + "accounting.transactions.approved")
                .description("Transactions approved by a parent").register(Telemetry.registry());
    }

    public void approved() {
        approved.increment();
    }
}
