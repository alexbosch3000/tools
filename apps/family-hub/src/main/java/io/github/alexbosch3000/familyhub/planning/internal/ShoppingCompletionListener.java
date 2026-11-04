// Marks the SHOPPING entries of a list as done when that list is completed.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.PlanningService;
import io.github.alexbosch3000.familyhub.shopping.ShoppingListCompleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Runs synchronously in the transaction completing the list. */
@Component
class ShoppingCompletionListener {

    private final PlanningService planning;

    ShoppingCompletionListener(PlanningService planning) {
        this.planning = planning;
    }

    @EventListener
    void on(ShoppingListCompleted event) {
        planning.closeShopping(event.listId());
    }
}
