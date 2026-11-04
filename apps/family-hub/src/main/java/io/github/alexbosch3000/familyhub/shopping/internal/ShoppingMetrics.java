// Shopping metrics: completed trips and amounts spent per kind of shop, open lists.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.shared.Telemetry;
import io.github.alexbosch3000.familyhub.shopping.ShoppingListCompleted;
import io.github.alexbosch3000.familyhub.shopping.ShoppingPayment;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ShoppingMetrics {

    ShoppingMetrics(ShoppingRepository repository) {
        Gauge.builder(Telemetry.PREFIX + "shopping.lists.open", repository, ShoppingRepository::countOpen)
                .description("Shopping lists not completed yet").register(Telemetry.registry());
    }

    @EventListener
    void on(ShoppingListCompleted event) {
        Counter.builder(Telemetry.PREFIX + "shopping.trips").description("Completed shopping trips")
                .register(Telemetry.registry()).increment();
        for (ShoppingPayment payment : event.payments()) {
            DistributionSummary.builder(Telemetry.PREFIX + "shopping.spent").baseUnit("euros")
                    .description("Amount paid per shopping trip and kind of shop")
                    .tag("store_type", payment.storeType().name())
                    .register(Telemetry.registry()).record(payment.amountCents() / 100.0);
        }
    }
}
