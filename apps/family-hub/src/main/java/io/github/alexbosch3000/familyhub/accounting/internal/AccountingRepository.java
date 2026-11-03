// SQL access to accounts, budget categories, ledger transactions and recurring transactions.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.Account;
import io.github.alexbosch3000.familyhub.accounting.AccountKind;
import io.github.alexbosch3000.familyhub.accounting.BudgetGroup;
import io.github.alexbosch3000.familyhub.accounting.Category;
import io.github.alexbosch3000.familyhub.accounting.Direction;
import io.github.alexbosch3000.familyhub.accounting.Period;
import io.github.alexbosch3000.familyhub.accounting.RecurringTransaction;
import io.github.alexbosch3000.familyhub.accounting.SourceType;
import io.github.alexbosch3000.familyhub.accounting.Transaction;
import io.github.alexbosch3000.familyhub.accounting.TransactionStatus;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AccountingRepository {

    private static final String SELECT_TRANSACTION = """
            SELECT t.*, c.direction, c.name AS category_name
            FROM ledger_transaction t JOIN budget_category c ON c.code = t.category_code""";

    private final JdbcClient jdbc;

    AccountingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Values written for a transaction; the direction is not stored, it comes from the category. */
    public record TransactionRow(LocalDate day, String label, long amountCents, String categoryCode, Long accountId,
            TransactionStatus status, SourceType sourceType, String sourceRef, String supplier, Long createdBy) {
    }

    // Accounts

    public List<Account> findAccounts() {
        return jdbc.sql("""
                SELECT a.id, a.name, a.kind, a.opening_balance_cents,
                       a.opening_balance_cents + COALESCE((
                           SELECT SUM(CASE c.direction WHEN 'INCOME' THEN t.amount_cents ELSE -t.amount_cents END)
                           FROM ledger_transaction t JOIN budget_category c ON c.code = t.category_code
                           WHERE t.account_id = a.id AND t.status = 'APPROVED'), 0) AS balance
                FROM account a ORDER BY a.name COLLATE NOCASE""")
                .query((rs, n) -> new Account(rs.getLong("id"), rs.getString("name"),
                        AccountKind.valueOf(rs.getString("kind")), rs.getLong("opening_balance_cents"),
                        rs.getLong("balance")))
                .list();
    }

    public long insertAccount(String name, AccountKind kind, long openingBalanceCents) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO account (name, kind, opening_balance_cents) VALUES (?, ?, ?)")
                .params(name, kind.name(), openingBalanceCents).update(keys);
        return keys.getKey().longValue();
    }

    public int updateAccount(long id, String name, AccountKind kind, long openingBalanceCents) {
        return jdbc.sql("UPDATE account SET name = ?, kind = ?, opening_balance_cents = ? WHERE id = ?")
                .params(name, kind.name(), openingBalanceCents, id).update();
    }

    public int deleteAccount(long id) {
        return jdbc.sql("DELETE FROM account WHERE id = ?").param(id).update();
    }

    // Categories

    public List<Category> findCategories() {
        return jdbc.sql("SELECT * FROM budget_category ORDER BY position, name")
                .query((rs, n) -> new Category(rs.getString("code"), rs.getString("name"),
                        Direction.valueOf(rs.getString("direction")),
                        Db.enumValue(rs, "budget_group", BudgetGroup.class),
                        Db.nullableLong(rs, "monthly_budget_cents"), rs.getInt("position")))
                .list();
    }

    public void insertCategory(Category category) {
        jdbc.sql("""
                INSERT INTO budget_category (code, name, direction, budget_group, monthly_budget_cents, position)
                VALUES (?, ?, ?, ?, ?, ?)""")
                .params(category.code(), category.name(), category.direction().name(), Db.text(category.group()),
                        category.monthlyBudgetCents(), category.position())
                .update();
    }

    public int updateCategory(String code, String name, BudgetGroup group, Long monthlyBudgetCents) {
        return jdbc.sql("""
                UPDATE budget_category SET name = ?, budget_group = ?, monthly_budget_cents = ?
                WHERE code = ?""")
                .params(name, Db.text(group), monthlyBudgetCents, code).update();
    }

    public int deleteCategory(String code) {
        return jdbc.sql("DELETE FROM budget_category WHERE code = ?").param(code).update();
    }

    // Transactions

    public List<Transaction> findTransactions(LocalDate from, LocalDate to) {
        return jdbc.sql(SELECT_TRANSACTION + " WHERE t.day >= ? AND t.day <= ? ORDER BY t.day, t.id")
                .params(Db.text(from), Db.text(to)).query(AccountingRepository::mapTransaction).list();
    }

    /** Pending transactions, and planned ones whose day has come. */
    public List<Transaction> findToApprove(LocalDate today) {
        return jdbc.sql(SELECT_TRANSACTION + " " + """
                 WHERE t.status = 'PENDING' OR (t.status = 'PLANNED' AND t.day <= ?)
                 ORDER BY t.day, t.id""")
                .param(Db.text(today)).query(AccountingRepository::mapTransaction).list();
    }

    public Optional<Transaction> findTransaction(long id) {
        return jdbc.sql(SELECT_TRANSACTION + " WHERE t.id = ?").param(id).query(AccountingRepository::mapTransaction)
                .optional();
    }

    public Optional<Transaction> findBySource(SourceType type, String ref) {
        return jdbc.sql(SELECT_TRANSACTION + " WHERE t.source_type = ? AND t.source_ref = ?")
                .params(type.name(), ref).query(AccountingRepository::mapTransaction).optional();
    }

    public long insertTransaction(TransactionRow row, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO ledger_transaction (day, label, amount_cents, category_code, account_id, status,
                    source_type, source_ref, supplier, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(Db.text(row.day()), row.label(), row.amountCents(), row.categoryCode(), row.accountId(),
                        row.status().name(), row.sourceType().name(), row.sourceRef(), row.supplier(),
                        row.createdBy(), Db.text(now))
                .update(keys);
        return keys.getKey().longValue();
    }

    /** Inserts unless a transaction with the same source already exists; returns whether it inserted. */
    public boolean insertTransactionIfAbsent(TransactionRow row, LocalDateTime now) {
        return jdbc.sql("""
                INSERT OR IGNORE INTO ledger_transaction (day, label, amount_cents, category_code, account_id, status,
                    source_type, source_ref, supplier, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(Db.text(row.day()), row.label(), row.amountCents(), row.categoryCode(), row.accountId(),
                        row.status().name(), row.sourceType().name(), row.sourceRef(), row.supplier(),
                        row.createdBy(), Db.text(now))
                .update() > 0;
    }

    public void updateTransaction(long id, LocalDate day, String label, long amountCents, String categoryCode,
            Long accountId, String supplier) {
        jdbc.sql("""
                UPDATE ledger_transaction SET day = ?, label = ?, amount_cents = ?, category_code = ?, account_id = ?,
                    supplier = ?
                WHERE id = ?""")
                .params(Db.text(day), label, amountCents, categoryCode, accountId, supplier, id).update();
    }

    public void setStatus(long id, TransactionStatus status) {
        jdbc.sql("UPDATE ledger_transaction SET status = ? WHERE id = ?").params(status.name(), id).update();
    }

    public void approve(long id, Long memberId, LocalDateTime now) {
        jdbc.sql("UPDATE ledger_transaction SET status = 'APPROVED', approved_by = ?, approved_at = ? WHERE id = ?")
                .params(memberId, Db.text(now), id).update();
    }

    public void deleteTransaction(long id) {
        jdbc.sql("DELETE FROM ledger_transaction WHERE id = ?").param(id).update();
    }

    public int deletePlanned(SourceType type, String ref) {
        return jdbc.sql("""
                DELETE FROM ledger_transaction
                WHERE source_type = ? AND source_ref = ? AND status = 'PLANNED'""")
                .params(type.name(), ref).update();
    }

    /** Deletes the planned transactions of a recurring transaction from a day on, before regenerating them. */
    public int deletePlannedRecurring(String refPrefix, LocalDate from) {
        return jdbc.sql("""
                DELETE FROM ledger_transaction
                WHERE source_type = 'RECURRING' AND status = 'PLANNED' AND substr(source_ref, 1, ?) = ? AND day >= ?""")
                .params(refPrefix.length(), refPrefix, Db.text(from)).update();
    }

    // Recurring transactions

    public List<RecurringTransaction> findRecurring() {
        return jdbc.sql("SELECT * FROM recurring_transaction ORDER BY label COLLATE NOCASE")
                .query(AccountingRepository::mapRecurring).list();
    }

    public Optional<RecurringTransaction> findRecurring(long id) {
        return jdbc.sql("SELECT * FROM recurring_transaction WHERE id = ?").param(id)
                .query(AccountingRepository::mapRecurring).optional();
    }

    public long insertRecurring(RecurringTransaction r) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO recurring_transaction (label, category_code, supplier, amount_cents, period, day_of_month,
                    start_date, end_date, account_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(r.label(), r.categoryCode(), r.supplier(), r.amountCents(), r.period().name(), r.dayOfMonth(),
                        Db.text(r.startDate()), Db.text(r.endDate()), r.accountId(), r.active())
                .update(keys);
        return keys.getKey().longValue();
    }

    public void updateRecurring(long id, RecurringTransaction r) {
        jdbc.sql("""
                UPDATE recurring_transaction SET label = ?, category_code = ?, supplier = ?, amount_cents = ?,
                    period = ?, day_of_month = ?, start_date = ?, end_date = ?, account_id = ?, active = ?
                WHERE id = ?""")
                .params(r.label(), r.categoryCode(), r.supplier(), r.amountCents(), r.period().name(), r.dayOfMonth(),
                        Db.text(r.startDate()), Db.text(r.endDate()), r.accountId(), r.active(), id)
                .update();
    }

    public void deleteRecurring(long id) {
        jdbc.sql("DELETE FROM recurring_transaction WHERE id = ?").param(id).update();
    }

    private static Transaction mapTransaction(ResultSet rs, int n) throws SQLException {
        return new Transaction(rs.getLong("id"), Db.date(rs, "day"), rs.getString("label"), rs.getLong("amount_cents"),
                Direction.valueOf(rs.getString("direction")), rs.getString("category_code"),
                rs.getString("category_name"), Db.nullableLong(rs, "account_id"),
                TransactionStatus.valueOf(rs.getString("status")), SourceType.valueOf(rs.getString("source_type")),
                rs.getString("source_ref"), rs.getString("supplier"), Db.nullableLong(rs, "created_by"),
                Db.nullableLong(rs, "approved_by"), Db.dateTime(rs, "approved_at"));
    }

    private static RecurringTransaction mapRecurring(ResultSet rs, int n) throws SQLException {
        return new RecurringTransaction(rs.getLong("id"), rs.getString("label"), rs.getString("category_code"),
                rs.getString("supplier"), rs.getLong("amount_cents"), Period.valueOf(rs.getString("period")),
                rs.getInt("day_of_month"), Db.date(rs, "start_date"), Db.date(rs, "end_date"),
                Db.nullableLong(rs, "account_id"), rs.getBoolean("active"));
    }
}
