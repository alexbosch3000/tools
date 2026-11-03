// SQL access to shopping lists, their recipe entries and their lines.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.shared.Db;
import io.github.alexbosch3000.familyhub.shopping.ListedRecipe;
import io.github.alexbosch3000.familyhub.shopping.ShoppingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ShoppingRepository {

    private final JdbcClient jdbc;

    ShoppingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Stored columns of a list, without its lines. */
    public record ListRow(long id, String name, ShoppingStatus status, LocalDate plannedFor, LocalDate completedOn,
            Long amountPaidCents) {
    }

    public Optional<ListRow> findList(long id) {
        return jdbc.sql("SELECT * FROM shopping_list WHERE id = ?").param(id)
                .query((rs, n) -> new ListRow(rs.getLong("id"), rs.getString("name"),
                        ShoppingStatus.valueOf(rs.getString("status")), Db.date(rs, "planned_for"),
                        Db.date(rs, "completed_on"), Db.nullableLong(rs, "amount_paid_cents")))
                .optional();
    }

    /** Open lists by planned date, then the most recently completed ones. */
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

    public long insertList(String name, LocalDate plannedFor, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO shopping_list (name, status, planned_for, created_at) VALUES (?, 'OPEN', ?, ?)")
                .params(name.trim(), Db.text(plannedFor), Db.text(now)).update(keys);
        return keys.getKey().longValue();
    }

    public void updateList(long id, String name, LocalDate plannedFor) {
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
                        Db.nullableLong(rs, "recipe_entry_id"),
                        new Quantity(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit"))),
                        rs.getBoolean("checked"), rs.getString("note")))
                .list();
    }

    public void insertLine(long listId, long itemId, Long recipeEntryId, Quantity quantity, String note) {
        jdbc.sql("""
                INSERT INTO shopping_line (list_id, item_id, recipe_entry_id, quantity, unit, note)
                VALUES (?, ?, ?, ?, ?, ?)""")
                .params(listId, itemId, recipeEntryId, quantity.amount(), quantity.unit().name(), note)
                .update();
    }

    public int deleteItemLines(long listId, long itemId) {
        return jdbc.sql("DELETE FROM shopping_line WHERE list_id = ? AND item_id = ?").params(listId, itemId)
                .update();
    }

    public int setChecked(long listId, long itemId, boolean checked) {
        return jdbc.sql("UPDATE shopping_line SET checked = ? WHERE list_id = ? AND item_id = ?")
                .params(checked, listId, itemId).update();
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
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO shopping_list_recipe (list_id, recipe_id, servings) VALUES (?, ?, ?)")
                .params(listId, recipeId, servings).update(keys);
        return keys.getKey().longValue();
    }

    /** Deletes a recipe entry; its lines go with it (ON DELETE CASCADE). */
    public int deleteRecipe(long listId, long entryId) {
        return jdbc.sql("DELETE FROM shopping_list_recipe WHERE list_id = ? AND id = ?").params(listId, entryId)
                .update();
    }
}
