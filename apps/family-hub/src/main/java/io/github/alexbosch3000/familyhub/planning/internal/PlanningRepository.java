// SQL access to planning entries, their attendees and their exceptions.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.AccountingAction;
import io.github.alexbosch3000.familyhub.planning.EntryType;
import io.github.alexbosch3000.familyhub.planning.EventCategory;
import io.github.alexbosch3000.familyhub.planning.Frequency;
import io.github.alexbosch3000.familyhub.planning.PlanningEntry;
import io.github.alexbosch3000.familyhub.planning.Recurrence;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlanningRepository {

    private final JdbcClient jdbc;

    PlanningRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PlanningEntry> findById(long id) {
        return load(jdbc.sql("SELECT * FROM planning_entry WHERE id = ?").param(id).query(Row::map).list())
                .stream().findFirst();
    }

    /** Recurring entries, and one-off entries overlapping {@code [from, to]}. */
    public List<PlanningEntry> findCandidates(LocalDate from, LocalDate to) {
        return load(jdbc.sql("""
                SELECT * FROM planning_entry
                WHERE recurrence_frequency IS NOT NULL OR (start_at < :rangeEnd AND end_at >= :rangeStart)
                ORDER BY start_at""")
                .param("rangeStart", Db.text(from.atStartOfDay()))
                .param("rangeEnd", Db.text(to.plusDays(1).atStartOfDay()))
                .query(Row::map).list());
    }

    public List<PlanningEntry> findAll() {
        return load(jdbc.sql("SELECT * FROM planning_entry ORDER BY start_at").query(Row::map).list());
    }

    public long insert(PlanningEntry entry, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO planning_entry (title, description, location, type, start_at, end_at, all_day,
                    recurrence_frequency, recurrence_interval, recurrence_days, recurrence_until, category,
                    estimated_cost_cents, shopping_list_id, recipe_id, servings, accounting_action, done, created_at)
                VALUES (:title, :description, :location, :type, :start, :end, :allDay, :frequency, :interval,
                    :days, :until, :category, :cost, :shoppingListId, :recipeId, :servings, :action, :done, :now)""")
                .params(params(entry)).param("now", Db.text(now))
                .update(keys);
        long id = keys.getKey().longValue();
        replaceChildren(id, entry);
        return id;
    }

    public void update(long id, PlanningEntry entry) {
        jdbc.sql("""
                UPDATE planning_entry SET title = :title, description = :description, location = :location,
                    type = :type, start_at = :start, end_at = :end, all_day = :allDay,
                    recurrence_frequency = :frequency, recurrence_interval = :interval, recurrence_days = :days,
                    recurrence_until = :until, category = :category, estimated_cost_cents = :cost,
                    shopping_list_id = :shoppingListId, recipe_id = :recipeId, servings = :servings,
                    accounting_action = :action, done = :done
                WHERE id = :id""")
                .params(params(entry)).param("id", id)
                .update();
        replaceChildren(id, entry);
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM planning_entry WHERE id = ?").param(id).update();
    }

    public void addException(long id, LocalDate day) {
        jdbc.sql("INSERT OR IGNORE INTO planning_exception (entry_id, day) VALUES (?, ?)")
                .params(id, Db.text(day)).update();
    }

    public void setDone(long id, boolean done) {
        jdbc.sql("UPDATE planning_entry SET done = ? WHERE id = ?").params(done, id).update();
    }

    public int markShoppingDone(long shoppingListId) {
        return jdbc.sql("UPDATE planning_entry SET done = 1 WHERE type = 'SHOPPING' AND shopping_list_id = ?")
                .param(shoppingListId).update();
    }

    private void replaceChildren(long id, PlanningEntry entry) {
        jdbc.sql("DELETE FROM planning_entry_member WHERE entry_id = ?").param(id).update();
        for (Long memberId : entry.memberIds()) {
            jdbc.sql("INSERT INTO planning_entry_member (entry_id, member_id) VALUES (?, ?)")
                    .params(id, memberId).update();
        }
        jdbc.sql("DELETE FROM planning_exception WHERE entry_id = ?").param(id).update();
        for (LocalDate day : entry.exceptions()) {
            addException(id, day);
        }
    }

    private static Map<String, Object> params(PlanningEntry e) {
        Recurrence r = e.recurrence();
        Map<String, Object> params = new HashMap<>();
        params.put("title", e.title());
        params.put("description", e.description() == null ? "" : e.description());
        params.put("location", e.location());
        params.put("type", e.type().name());
        params.put("start", Db.text(e.start()));
        params.put("end", Db.text(e.end()));
        params.put("allDay", e.allDay());
        params.put("frequency", r == null ? null : r.frequency().name());
        params.put("interval", r == null ? null : r.interval());
        params.put("days", r == null ? null : r.days().stream().map(DayOfWeek::name).collect(Collectors.joining(",")));
        params.put("until", r == null ? null : Db.text(r.until()));
        params.put("category", Db.text(e.category()));
        params.put("cost", e.estimatedCostCents());
        params.put("shoppingListId", e.shoppingListId());
        params.put("recipeId", e.recipeId());
        params.put("servings", e.servings());
        params.put("action", Db.text(e.accountingAction()));
        params.put("done", e.done());
        return params;
    }

    /** Completes stored rows with their attendees and exceptions. */
    private List<PlanningEntry> load(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(Row::id).toList();
        Map<Long, List<Long>> members = new HashMap<>();
        jdbc.sql("SELECT entry_id, member_id FROM planning_entry_member WHERE entry_id IN (:ids) ORDER BY member_id")
                .param("ids", ids)
                .query(rs -> {
                    members.computeIfAbsent(rs.getLong("entry_id"), k -> new ArrayList<>())
                            .add(rs.getLong("member_id"));
                });
        Map<Long, List<LocalDate>> exceptions = new HashMap<>();
        jdbc.sql("SELECT entry_id, day FROM planning_exception WHERE entry_id IN (:ids) ORDER BY day")
                .param("ids", ids)
                .query(rs -> {
                    exceptions.computeIfAbsent(rs.getLong("entry_id"), k -> new ArrayList<>())
                            .add(LocalDate.parse(rs.getString("day")));
                });
        return rows.stream().map(row -> row.toEntry(members.getOrDefault(row.id(), List.of()),
                exceptions.getOrDefault(row.id(), List.of()))).toList();
    }

    /** A planning_entry row before its children are attached. */
    private record Row(long id, String title, String description, String location, EntryType type,
            LocalDateTime start, LocalDateTime end, boolean allDay, Recurrence recurrence, EventCategory category,
            Long cost, Long shoppingListId, Long recipeId, Integer servings, AccountingAction action, boolean done) {

        static Row map(ResultSet rs, int n) throws SQLException {
            String frequency = rs.getString("recurrence_frequency");
            Recurrence recurrence = frequency == null ? null : new Recurrence(Frequency.valueOf(frequency),
                    rs.getInt("recurrence_interval"),
                    Db.csv(rs, "recurrence_days").stream().map(DayOfWeek::valueOf).toList(),
                    Db.date(rs, "recurrence_until"));
            return new Row(rs.getLong("id"), rs.getString("title"), rs.getString("description"),
                    rs.getString("location"), EntryType.valueOf(rs.getString("type")),
                    Db.dateTime(rs, "start_at"), Db.dateTime(rs, "end_at"), rs.getBoolean("all_day"), recurrence,
                    Db.enumValue(rs, "category", EventCategory.class), Db.nullableLong(rs, "estimated_cost_cents"),
                    Db.nullableLong(rs, "shopping_list_id"), Db.nullableLong(rs, "recipe_id"),
                    Db.nullableInt(rs, "servings"), Db.enumValue(rs, "accounting_action", AccountingAction.class),
                    rs.getBoolean("done"));
        }

        PlanningEntry toEntry(List<Long> memberIds, List<LocalDate> exceptions) {
            return new PlanningEntry(id, title, description, location, type, start, end, allDay, recurrence,
                    exceptions, memberIds, category, cost, shoppingListId, recipeId, servings, action, done);
        }
    }
}
