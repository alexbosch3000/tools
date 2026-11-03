// Plans the recurring transactions of the current and next month at startup and every night.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.AccountingService;
import java.time.Clock;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RecurringScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RecurringScheduler.class);

    private final AccountingService accounting;
    private final Clock clock;

    RecurringScheduler(AccountingService accounting, Clock clock) {
        this.accounting = accounting;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onStartup() {
        run();
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "${app.time-zone}")
    void nightly() {
        run();
    }

    private void run() {
        YearMonth month = YearMonth.now(clock);
        int created = accounting.generate(month) + accounting.generate(month.plusMonths(1));
        if (created > 0) {
            LOG.info("Planned {} recurring transactions", created);
        }
    }
}
