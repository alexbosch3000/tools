// SQL access to shopping lists, their recipe entries and their lines.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.shared.Db;
import io.github.alexbosch3000.familyhub.shopping.ListedRecipe;
import io.github.alexbosch3000.familyhub.shopping.Purchase;
import io.github.alexbosch3000.familyhub.shopping.ShoppingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ShoppingRepository {

    private final JdbcClient jdbc;

    ShoppingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Stored columns of a list, without its lines. */
    public record ListRow(long id, String name, ShoppingStatus status, @Nullable LocalDate plannedFor,
            @Nullable LocalDate completedOn, @Nullable Long amountPaidCents) {
    }

    public Optional<ListRow> findList(long id) {
        return jdbc.sql("SELECT * FROM shopping_list WHERE id = ?").param(id)
                .query((rs, n) -> new ListRow(rs.getLong("id"), rs.getString("name"),
                        ShoppingStatus.valueOf(rs.getString("status")), Db.date(rs, "planned_for"),
                        Db.date(rs, "completed_on"), Db.nullableLong(rs, "amount_paid_cents")))
                .optional();
    }

    public long countOpen() {
        Long count = jdbc.sql("SELECT COUNT(*) FROM shopping_list WHERE status = 'OPEN'").query(Long.class).single();
        return count == null ? 0 : count;
    }

    /** Open lists by planned date, or the most recently completed ones. */
    public List<Long> findListIds(ShoppingStatus status, int doneLimit) {
        if (status == ShoppingStatus.OPEN) {
            return jdbc.sql("""
                    SELECT id FROM shopping_list WHERE status = 'OPEN'
                    ORDER BY planned_for IS NULL, planned_for, id DESC""").query(Long.class).list();
        }
        return jdbc.sql("""
                SELECT id FROM shopping_list WHERE status = 'DONE' ORDER BY completed_on DESC, id DESC LIMIT ?""")
                .param(doneLimit).query(Long.class).list();
    }

    public long insertList(String name, @Nullable LocalDate plannedFor, LocalDateTime now) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO shopping_list (name, status, planned_for, created_at) VALUES (?, 'OPEN', ?, ?)")
                .params(name.trim(), Db.text(plannedFor), Db.text(now)).update(keys);
        return Db.key(keys);
    }

    public void updateList(long id, String name, @Nullable LocalDate plannedFor) {
        jdbc.sql("UPDATE shopping_list SET name = ?, planned_for = ? WHERE id = ?")
                .params(name.trim(), Db.text(plannedFor), id).update();
    }

    public void complete(long id, LocalDate paidOn, long amountCents) {
        jdbc.sql("UPDATE shopping_list SET status = 'DONE', completed_on = ?, amount_paid_cents = ? WHERE id = ?")
                .params(Db.text(paidOn), amountCents, id).update();
    }

    public void deleteList(long id) {
        jdbc.sql("DELETE FROM shopping_list WHERE id = ?").param(id).update();
    }

    public List<ShoppingLine> findLines(long listId) {
        return jdbc.sql("SELECT * FROM shopping_line WHERE list_id = ? ORDER BY id").param(listId)
                .query((rs, n) -> new ShoppingLine(rs.getLong("id"), rs.getLong("item_id"),
                        Db.nullableLong(rs, "recipe_entry_id"), Db.nullableLong(rs, "member_id"),
                        new Quantity(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit"))),
                        rs.getBoolean("checked"), rs.getString("note")))
                .list();
    }

    public void insertLine(long listId, long itemId, @Nullable Long recipeEntryId, @Nullable Long memberId,
            Quantity quantity, @Nullable String note) {
        jdbc.sql("""
                INSERT INTO shopping_line (list_id, item_id, recipe_entry_id, member_id, quantity, unit, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")
                .params(listId, itemId, recipeEntryId, memberId, quantity.amount(), quantity.unit().name(), note)
                .update();
    }

    /** Deletes the lines of a row: an item for a member, or for the household when memberId is null. */
    public int deleteRowLines(long listId, long itemId, @Nullable Long memberId) {
        return jdbc.sql("DELETE FROM shopping_line WHERE list_id = ? AND item_id = ? AND member_id IS ?")
                .params(listId, itemId, memberId).update();
    }

    public int setRowChecked(long listId, long itemId, @Nullable Long memberId, boolean checked) {
        return jdbc.sql("UPDATE shopping_line SET checked = ? WHERE list_id = ? AND item_id = ? AND member_id IS ?")
                .params(checked, listId, itemId, memberId).update();
    }

    public boolean rowExists(long listId, long itemId, @Nullable Long memberId) {
        Integer count = jdbc.sql(
                "SELECT COUNT(*) FROM shopping_line WHERE list_id = ? AND item_id = ? AND member_id IS ?")
                .params(listId, itemId, memberId).query(Integer.class).single();
        return count != null && count > 0;
    }

    public Map<ShoppingRows.RowKey, Purchase> findPurchases(long listId) {
        Map<ShoppingRows.RowKey, Purchase> purchases = new HashMap<>();
        jdbc.sql("SELECT * FROM shopping_purchase WHERE list_id = ?").param(listId).query(rs -> {
            purchases.put(new ShoppingRows.RowKey(rs.getLong("item_id"), Db.nullableLong(rs, "member_id")),
                    new Purchase(new Quantity(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit"))),
                            Db.nullableLong(rs, "price_cents"), rs.getString("note")));
        });
        return purchases;
    }

    public void savePurchase(long listId, long itemId, @Nullable Long memberId, Purchase purchase) {
        deletePurchase(listId, itemId, memberId);
        jdbc.sql("""
                INSERT INTO shopping_purchase (list_id, item_id, member_id, quantity, unit, price_cents, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)""")
                .params(listId, itemId, memberId, purchase.quantity().amount(), purchase.quantity().unit().name(),
                        purchase.priceCents(), purchase.note())
                .update();
    }

    public int deletePurchase(long listId, long itemId, @Nullable Long memberId) {
        return jdbc.sql("DELETE FROM shopping_purchase WHERE list_id = ? AND item_id = ? AND member_id IS ?")
                .params(listId, itemId, memberId).update();
    }

    /** Deletes the purchases of rows that no longer have any line, after a recipe was removed. */
    public void deleteOrphanPurchases(long listId) {
        jdbc.sql("""
                DELETE FROM shopping_purchase WHERE list_id = ? AND NOT EXISTS (SELECT 1 FROM shopping_line l
                  WHERE l.list_id = shopping_purchase.list_id AND l.item_id = shopping_purchase.item_id
                    AND l.member_id IS shopping_purchase.member_id)""").param(listId).update();
    }

    public List<ListedRecipe> findRecipes(long listId) {
        return jdbc.sql("""
                SELECT e.id, e.recipe_id, r.name, e.servings
                FROM shopping_list_recipe e JOIN recipe r ON r.id = e.recipe_id
                WHERE e.list_id = ? ORDER BY e.id""")
                .param(listId)
                .query((rs, n) -> new ListedRecipe(rs.getLong("id"), rs.getLong("recipe_id"), rs.getString("name"),
                        rs.getInt("servings")))
                .list();
    }

    public long insertRecipe(long listId, long recipeId, int servings) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO shopping_list_recipe (list_id, recipe_id, servings) VALUES (?, ?, ?)")
                .params(listId, recipeId, servings).update(keys);
        return Db.key(keys);
    }

    /** Deletes a recipe entry; its lines go with it (ON DELETE CASCADE). */
    public int deleteRecipe(long listId, long entryId) {
        return jdbc.sql("DELETE FROM shopping_list_recipe WHERE list_id = ? AND id = ?").params(listId, entryId)
                .update();
    }
}
