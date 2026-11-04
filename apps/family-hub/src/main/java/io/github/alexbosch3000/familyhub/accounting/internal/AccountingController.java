// Accounting API, restricted to parents: accounts, categories, transactions, approvals, recurring items, reports.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.Account;
import io.github.alexbosch3000.familyhub.accounting.AccountKind;
import io.github.alexbosch3000.familyhub.accounting.AccountingService;
import io.github.alexbosch3000.familyhub.accounting.AccountingService.Approval;
import io.github.alexbosch3000.familyhub.accounting.AccountingService.RecurringData;
import io.github.alexbosch3000.familyhub.accounting.AccountingService.TransactionData;
import io.github.alexbosch3000.familyhub.accounting.BudgetGroup;
import io.github.alexbosch3000.familyhub.accounting.Category;
import io.github.alexbosch3000.familyhub.accounting.Direction;
import io.github.alexbosch3000.familyhub.accounting.MonthlyReport;
import io.github.alexbosch3000.familyhub.accounting.Period;
import io.github.alexbosch3000.familyhub.accounting.RecurringTransaction;
import io.github.alexbosch3000.familyhub.accounting.Transaction;
import io.github.alexbosch3000.familyhub.accounting.TransactionStatus;
import io.github.alexbosch3000.familyhub.accounting.YearlyReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounting")
class AccountingController {

    private final AccountingService accounting;

    AccountingController(AccountingService accounting) {
        this.accounting = accounting;
    }

    record AccountRequest(@NotBlank @Size(max = 60) String name, @NotNull AccountKind kind, long openingBalanceCents) {
    }

    record CategoryRequest(@NotBlank @Size(max = 60) String name, @Nullable Direction direction,
            @Nullable BudgetGroup group, @Nullable @PositiveOrZero Long monthlyBudgetCents) {
    }

    record TransactionRequest(@NotNull LocalDate day, @NotBlank @Size(max = 120) String label,
            @PositiveOrZero long amountCents, @NotBlank String categoryCode, @Nullable Long accountId,
            @Nullable @Size(max = 80) String supplier, @Nullable TransactionStatus status) {

        TransactionData toData() {
            return new TransactionData(day, label, amountCents, categoryCode, accountId, supplier, status);
        }
    }

    record ApprovalRequest(@Nullable @PositiveOrZero Long amountCents, @Nullable LocalDate day,
            @Nullable String categoryCode, @Nullable Long accountId) {
    }

    record RecurringRequest(@NotBlank @Size(max = 120) String label, @NotBlank String categoryCode,
            @Nullable @Size(max = 80) String supplier, @PositiveOrZero long amountCents, @NotNull Period period,
            @Min(1) @Max(31) int dayOfMonth, @NotNull LocalDate startDate, @Nullable LocalDate endDate,
            @Nullable Long accountId, boolean active) {

        RecurringData toData() {
            return new RecurringData(label, categoryCode, supplier, amountCents, period, dayOfMonth, startDate, endDate,
                    accountId, active);
        }
    }

    record Generated(int created) {
    }

    // Accounts

    @GetMapping("/accounts")
    List<Account> accounts() {
        return accounting.accounts();
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    Account createAccount(@Valid @RequestBody AccountRequest request) {
        return accounting.createAccount(request.name(), request.kind(), request.openingBalanceCents());
    }

    @PutMapping("/accounts/{id}")
    Account updateAccount(@PathVariable long id, @Valid @RequestBody AccountRequest request) {
        return accounting.updateAccount(id, request.name(), request.kind(), request.openingBalanceCents());
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(@PathVariable long id) {
        accounting.deleteAccount(id);
    }

    // Categories

    @GetMapping("/categories")
    List<Category> categories() {
        return accounting.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    Category createCategory(@Valid @RequestBody CategoryRequest request) {
        Direction direction = request.direction() == null ? Direction.EXPENSE : request.direction();
        return accounting.createCategory(request.name(), direction, request.group(), request.monthlyBudgetCents());
    }

    @PutMapping("/categories/{code}")
    Category updateCategory(@PathVariable String code, @Valid @RequestBody CategoryRequest request) {
        return accounting.updateCategory(code, request.name(), request.group(), request.monthlyBudgetCents());
    }

    @DeleteMapping("/categories/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable String code) {
        accounting.deleteCategory(code);
    }

    // Transactions

    @GetMapping("/transactions")
    List<Transaction> transactions(@RequestParam YearMonth month) {
        return accounting.transactions(month);
    }

    @GetMapping("/approvals")
    List<Transaction> approvals() {
        return accounting.toApprove();
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    Transaction create(@Valid @RequestBody TransactionRequest request) {
        return accounting.create(request.toData());
    }

    @PutMapping("/transactions/{id}")
    Transaction update(@PathVariable long id, @Valid @RequestBody TransactionRequest request) {
        return accounting.update(id, request.toData());
    }

    @PostMapping("/transactions/{id}/approve")
    Transaction approve(@PathVariable long id, @Valid @RequestBody ApprovalRequest request) {
        return accounting.approve(id, new Approval(request.amountCents(), request.day(), request.categoryCode(),
                request.accountId()));
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        accounting.delete(id);
    }

    // Recurring transactions

    @GetMapping("/recurring")
    List<RecurringTransaction> recurring() {
        return accounting.recurring();
    }

    @PostMapping("/recurring")
    @ResponseStatus(HttpStatus.CREATED)
    RecurringTransaction createRecurring(@Valid @RequestBody RecurringRequest request) {
        return accounting.createRecurring(request.toData());
    }

    @PutMapping("/recurring/{id}")
    RecurringTransaction updateRecurring(@PathVariable long id, @Valid @RequestBody RecurringRequest request) {
        return accounting.updateRecurring(id, request.toData());
    }

    @DeleteMapping("/recurring/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRecurring(@PathVariable long id) {
        accounting.deleteRecurring(id);
    }

    @PostMapping("/recurring/generate")
    Generated generate(@RequestParam YearMonth month) {
        return new Generated(accounting.generate(month));
    }

    // Reports

    @GetMapping("/reports/monthly")
    MonthlyReport monthly(@RequestParam YearMonth month) {
        return accounting.monthlyReport(month);
    }

    @GetMapping("/reports/yearly")
    YearlyReport yearly(@RequestParam @Min(1900) @Max(2200) int year) {
        return accounting.yearlyReport(year);
    }
}
