// Public API of the accounting module: accounts, envelopes, transactions and their approval, reports.
package io.github.alexbosch3000.familyhub.accounting;

import io.github.alexbosch3000.familyhub.accounting.internal.AccountingMetrics;
import io.github.alexbosch3000.familyhub.accounting.internal.AccountingRepository;
import io.github.alexbosch3000.familyhub.accounting.internal.AccountingRepository.TransactionRow;
import io.github.alexbosch3000.familyhub.accounting.internal.RecurringSchedule;
import io.github.alexbosch3000.familyhub.accounting.internal.ReportCalculator;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountingService.class);
    private static final int UPCOMING_DAYS = 30;
    private static final int UPCOMING_SHOWN = 5;

    private final AccountingRepository repository;
    private final MemberService members;
    private final AccountingMetrics metrics;
    private final Clock clock;

    AccountingService(AccountingRepository repository, MemberService members, AccountingMetrics metrics,
            Clock clock) {
        this.repository = repository;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record TransactionData(LocalDate day, String label, long amountCents, String categoryCode,
            @Nullable Long accountId, @Nullable String supplier, @Nullable TransactionStatus status) {
    }

    /** Corrections applied when approving; null fields keep the current values. */
    public record Approval(@Nullable Long amountCents, @Nullable LocalDate day, @Nullable String categoryCode,
            @Nullable Long accountId) {
    }

    public record RecurringData(String label, String categoryCode, @Nullable String supplier, long amountCents,
            Period period, int dayOfMonth, LocalDate startDate, @Nullable LocalDate endDate, @Nullable Long accountId,
            boolean active) {
    }

    // Accounts

    public List<Account> accounts() {
        return repository.findAccounts();
    }

    @Transactional
    public Account createAccount(String name, AccountKind kind, long openingBalanceCents) {
        long id = repository.insertAccount(name.trim(), kind, openingBalanceCents);
        return account(id);
    }

    @Transactional
    public Account updateAccount(long id, String name, AccountKind kind, long openingBalanceCents) {
        if (repository.updateAccount(id, name.trim(), kind, openingBalanceCents) == 0) {
            throw new NotFoundException("Compte introuvable.");
        }
        return account(id);
    }

    /** Fails with a conflict while transactions reference the account. */
    @Transactional
    public void deleteAccount(long id) {
        if (repository.deleteAccount(id) == 0) {
            throw new NotFoundException("Compte introuvable.");
        }
    }

    // Categories

    public List<Category> categories() {
        return repository.findCategories();
    }

    @Transactional
    public Category createCategory(String name, Direction direction, @Nullable BudgetGroup group,
            @Nullable Long monthlyBudgetCents) {
        validateGroup(direction, group);
        List<Category> existing = categories();
        String code = uniqueCode(slug(name), existing);
        int position = existing.stream().mapToInt(Category::position).max().orElse(0) + 1;
        repository.insertCategory(new Category(code, name.trim(), direction, group,
                direction == Direction.INCOME ? null : monthlyBudgetCents, position));
        return category(code);
    }

    @Transactional
    public Category updateCategory(String code, String name, @Nullable BudgetGroup group,
            @Nullable Long monthlyBudgetCents) {
        Category current = category(code);
        validateGroup(current.direction(), group);
        repository.updateCategory(code, name.trim(), group,
                current.direction() == Direction.INCOME ? null : monthlyBudgetCents);
        return category(code);
    }

    /** Fails with a conflict while transactions reference the category. */
    @Transactional
    public void deleteCategory(String code) {
        if (repository.deleteCategory(code) == 0) {
            throw new NotFoundException("Catégorie introuvable.");
        }
    }

    // Transactions

    public List<Transaction> transactions(YearMonth month) {
        return repository.findTransactions(month.atDay(1), month.atEndOfMonth());
    }

    /** What a parent has to approve today: pending transactions and planned ones that are due. */
    public List<Transaction> toApprove() {
        return repository.findToApprove(LocalDate.now(clock));
    }

    public Transaction transaction(long id) {
        return repository.findTransaction(id).orElseThrow(() -> new NotFoundException("Opération introuvable."));
    }

    @Transactional
    public Transaction create(TransactionData data) {
        validateReferences(data.categoryCode(), data.accountId());
        TransactionStatus status = data.status() == null ? TransactionStatus.PENDING : data.status();
        Long memberId = members.currentMemberId().orElse(null);
        long id = repository.insertTransaction(new TransactionRow(data.day(), data.label().trim(), data.amountCents(),
                data.categoryCode(), data.accountId(), status, SourceType.MANUAL, null, blankToNull(data.supplier()),
                memberId), LocalDateTime.now(clock));
        if (status == TransactionStatus.APPROVED) {
            repository.approve(id, memberId, LocalDateTime.now(clock));
        }
        return transaction(id);
    }

    @Transactional
    public Transaction update(long id, TransactionData data) {
        transaction(id);
        validateReferences(data.categoryCode(), data.accountId());
        repository.updateTransaction(id, data.day(), data.label().trim(), data.amountCents(), data.categoryCode(),
                data.accountId(), blankToNull(data.supplier()));
        return transaction(id);
    }

    /** Approves a planned or pending transaction, optionally correcting it with the actual values. */
    @Transactional
    @WithSpan
    public Transaction approve(@SpanAttribute("familyhub.transaction.id") long id, Approval approval) {
        Transaction current = transaction(id);
        if (current.status() == TransactionStatus.APPROVED) {
            throw new ConflictException("Cette opération est déjà approuvée.");
        }
        String category = approval.categoryCode() != null ? approval.categoryCode() : current.categoryCode();
        Long account = approval.accountId() != null ? approval.accountId() : current.accountId();
        validateReferences(category, account);
        repository.updateTransaction(id, approval.day() != null ? approval.day() : current.day(), current.label(),
                approval.amountCents() != null ? approval.amountCents() : current.amountCents(), category, account,
                current.supplier());
        Long approver = members.currentMemberId().orElse(null);
        repository.approve(id, approver, LocalDateTime.now(clock));
        metrics.approved();
        LOG.atInfo().addKeyValue("transaction.id", id).addKeyValue("member.id", approver)
                .log("Transaction approved");
        return transaction(id);
    }

    @Transactional
    public void delete(long id) {
        transaction(id);
        repository.deleteTransaction(id);
    }

    // Recurring transactions

    public List<RecurringTransaction> recurring() {
        return repository.findRecurring();
    }

    @Transactional
    public RecurringTransaction createRecurring(RecurringData data) {
        validateRecurring(data);
        long id = repository.insertRecurring(toRecurring(0, data));
        regenerate(id);
        return recurring(id);
    }

    /** Replaces the not yet approved transactions it planned from this month on. */
    @Transactional
    public RecurringTransaction updateRecurring(long id, RecurringData data) {
        recurring(id);
        validateRecurring(data);
        repository.updateRecurring(id, toRecurring(id, data));
        regenerate(id);
        return recurring(id);
    }

    @Transactional
    public void deleteRecurring(long id) {
        recurring(id);
        repository.deletePlannedRecurring(RecurringSchedule.sourceRefPrefix(id), YearMonth.now(clock).atDay(1));
        repository.deleteRecurring(id);
    }

    /** Plans the transactions falling due in a month; idempotent. Returns how many were created. */
    @Transactional
    @WithSpan
    public int generate(YearMonth month) {
        int created = 0;
        for (RecurringTransaction r : repository.findRecurring()) {
            created += generate(r, month) ? 1 : 0;
        }
        return created;
    }

    // Reports

    @WithSpan
    public MonthlyReport monthlyReport(YearMonth month) {
        return ReportCalculator.monthly(month, categories(), transactions(month));
    }

    public YearlyReport yearlyReport(int year) {
        return ReportCalculator.yearly(year,
                repository.findTransactions(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    public AccountingSummary summary() {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        MonthlyReport report = monthlyReport(month);
        List<Transaction> upcoming = repository.findTransactions(today, today.plusDays(UPCOMING_DAYS)).stream()
                .filter(t -> t.status() != TransactionStatus.APPROVED)
                .limit(UPCOMING_SHOWN)
                .toList();
        return new AccountingSummary(month, report.expenseActualCents(), report.expenseForecastCents(),
                report.budgetTotalCents(), toApprove().size(), upcoming, accounts());
    }

    private boolean generate(RecurringTransaction r, YearMonth month) {
        return RecurringSchedule.dueDate(r, month)
                .map(day -> repository.insertTransactionIfAbsent(new TransactionRow(day, r.label(), r.amountCents(),
                        r.categoryCode(), r.accountId(), TransactionStatus.PLANNED, SourceType.RECURRING,
                        RecurringSchedule.sourceRef(r.id(), month), r.supplier(), null), LocalDateTime.now(clock)))
                .orElse(false);
    }

    private void regenerate(long id) {
        YearMonth month = YearMonth.now(clock);
        repository.deletePlannedRecurring(RecurringSchedule.sourceRefPrefix(id), month.atDay(1));
        RecurringTransaction r = recurring(id);
        generate(r, month);
        generate(r, month.plusMonths(1));
    }

    private Account account(long id) {
        return accounts().stream().filter(a -> a.id() == id).findFirst()
                .orElseThrow(() -> new NotFoundException("Compte introuvable."));
    }

    private Category category(String code) {
        return categories().stream().filter(c -> c.code().equals(code)).findFirst()
                .orElseThrow(() -> new NotFoundException("Catégorie introuvable."));
    }

    private RecurringTransaction recurring(long id) {
        return repository.findRecurring(id).orElseThrow(() -> new NotFoundException("Échéance introuvable."));
    }

    private void validateReferences(String categoryCode, @Nullable Long accountId) {
        category(categoryCode);
        if (accountId != null) {
            account(accountId);
        }
    }

    private void validateRecurring(RecurringData data) {
        validateReferences(data.categoryCode(), data.accountId());
        if (data.endDate() != null && data.endDate().isBefore(data.startDate())) {
            throw new InvalidRequestException("La date de fin précède la date de début.");
        }
    }

    private static void validateGroup(Direction direction, @Nullable BudgetGroup group) {
        if ((direction == Direction.EXPENSE) != (group != null)) {
            throw new InvalidRequestException(
                    "Une dépense a un groupe (besoins, envies, épargne), un revenu n'en a pas.");
        }
    }

    private static RecurringTransaction toRecurring(long id, RecurringData d) {
        return new RecurringTransaction(id, d.label().trim(), d.categoryCode(), blankToNull(d.supplier()),
                d.amountCents(), d.period(), d.dayOfMonth(), d.startDate(), d.endDate(), d.accountId(), d.active());
    }

    /** Lower-case ASCII code derived from a name: "Cantine & goûters" gives "cantine-gouters". */
    static String slug(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "categorie" : slug;
    }

    private static String uniqueCode(String base, List<Category> existing) {
        String code = base;
        for (int i = 2; codeTaken(code, existing); i++) {
            code = base + "-" + i;
        }
        return code;
    }

    private static boolean codeTaken(String code, List<Category> existing) {
        return existing.stream().anyMatch(c -> c.code().equals(code));
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
