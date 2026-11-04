// Records in the ledger what other modules report: planned costs of planning entries, completed shopping trips.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.Category;
import io.github.alexbosch3000.familyhub.accounting.Direction;
import io.github.alexbosch3000.familyhub.accounting.SourceType;
import io.github.alexbosch3000.familyhub.accounting.Transaction;
import io.github.alexbosch3000.familyhub.accounting.TransactionStatus;
import io.github.alexbosch3000.familyhub.accounting.internal.AccountingRepository.TransactionRow;
import io.github.alexbosch3000.familyhub.planning.PlannedExpenseCancelled;
import io.github.alexbosch3000.familyhub.planning.PlannedExpenseChanged;
import io.github.alexbosch3000.familyhub.shopping.ShoppingListCompleted;
import io.github.alexbosch3000.familyhub.shopping.ShoppingPayment;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listeners run synchronously in the publisher's transaction. Transactions already pending or
 * approved are never overwritten by a later change of the planning entry: they record what happened.
 */
@Component
class AccountingEventListener {

    private final AccountingRepository repository;
    private final Clock clock;

    AccountingEventListener(AccountingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @EventListener
    void on(PlannedExpenseChanged event) {
        String ref = planningRef(event.entryId());
        String category = expenseCategory(CategoryMapping.forEvent(event.category()));
        Optional<Transaction> existing = repository.findBySource(SourceType.PLANNING, ref);
        if (existing.isEmpty()) {
            repository.insertTransaction(new TransactionRow(event.day(), event.title(), event.amountCents(), category,
                    null, TransactionStatus.PLANNED, SourceType.PLANNING, ref, null, null), LocalDateTime.now(clock));
        } else if (existing.get().status() == TransactionStatus.PLANNED) {
            Transaction t = existing.get();
            repository.updateTransaction(t.id(), event.day(), event.title(), event.amountCents(), category,
                    t.accountId(), t.supplier());
        }
    }

    @EventListener
    void on(PlannedExpenseCancelled event) {
        repository.deletePlanned(SourceType.PLANNING, planningRef(event.entryId()));
    }

    /** One pending expense per kind of shop, so that clothes and groceries land in their own categories. */
    @EventListener
    void on(ShoppingListCompleted event) {
        Long account = event.accountId() != null && repository.findAccounts().stream()
                .anyMatch(a -> a.id() == event.accountId()) ? event.accountId() : null;
        for (ShoppingPayment payment : event.payments()) {
            String ref = "list-" + event.listId() + "-" + payment.storeType().name();
            if (payment.amountCents() == 0 || repository.findBySource(SourceType.SHOPPING, ref).isPresent()) {
                continue;
            }
            repository.insertTransaction(new TransactionRow(event.paidOn(), event.name(), payment.amountCents(),
                    expenseCategory(CategoryMapping.forStore(payment.storeType())), account,
                    TransactionStatus.PENDING, SourceType.SHOPPING, ref, null, null), LocalDateTime.now(clock));
        }
    }

    /** The mapped category if the family kept it, else the fallback, else the first expense category. */
    private String expenseCategory(String preferred) {
        List<Category> expenses = repository.findCategories().stream()
                .filter(c -> c.direction() == Direction.EXPENSE).toList();
        return expenses.stream().map(Category::code).filter(preferred::equals).findFirst()
                .or(() -> expenses.stream().map(Category::code).filter(CategoryMapping.FALLBACK::equals).findFirst())
                .or(() -> expenses.stream().map(Category::code).findFirst())
                .orElseThrow(() -> new IllegalStateException("No expense category left"));
    }

    private static String planningRef(long entryId) {
        return "entry-" + entryId;
    }
}
