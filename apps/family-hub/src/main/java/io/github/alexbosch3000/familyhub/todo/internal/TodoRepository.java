// SQL access to to-do lists and items.
package io.github.alexbosch3000.familyhub.todo.internal;

import io.github.alexbosch3000.familyhub.shared.Db;
import io.github.alexbosch3000.familyhub.todo.Priority;
import io.github.alexbosch3000.familyhub.todo.Repeat;
import io.github.alexbosch3000.familyhub.todo.TodoItem;
import io.github.alexbosch3000.familyhub.todo.TodoList;
import io.github.alexbosch3000.familyhub.todo.TodoService.ItemData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TodoRepository {

    private final JdbcClient jdbc;

    TodoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TodoList> findLists() {
        return jdbc.sql("""
                SELECT l.id, l.name, (SELECT COUNT(*) FROM todo_item i WHERE i.list_id = l.id AND i.done = 0) AS open
                FROM todo_list l ORDER BY l.name COLLATE NOCASE""")
                .query((rs, n) -> new TodoList(rs.getLong("id"), rs.getString("name"), rs.getInt("open")))
                .list();
    }

    public boolean listNameTaken(String name, Long exceptId) {
        return jdbc.sql("SELECT COUNT(*) FROM todo_list WHERE name = ? AND id IS NOT ?").params(name, exceptId)
                .query(Long.class).single() > 0;
    }

    public long insertList(String name, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO todo_list (name, created_at) VALUES (?, ?)").params(name, Db.text(now)).update(keys);
        return keys.getKey().longValue();
    }

    public void renameList(long id, String name) {
        jdbc.sql("UPDATE todo_list SET name = ? WHERE id = ?").params(name, id).update();
    }

    public void deleteList(long id) {
        jdbc.sql("DELETE FROM todo_list WHERE id = ?").param(id).update();
    }

    public List<TodoItem> findItems(Long listId, Long assigneeId, boolean includeDone) {
        return jdbc.sql("""
                SELECT * FROM todo_item
                WHERE (:listId IS NULL OR list_id = :listId)
                  AND (:assigneeId IS NULL OR assignee_id = :assigneeId)
                  AND (:includeDone OR done = 0)
                ORDER BY done, CASE WHEN done = 0 THEN due_at IS NULL END, due_at,
                         CASE priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, done_at DESC, id""")
                .param("listId", listId).param("assigneeId", assigneeId).param("includeDone", includeDone)
                .query(TodoRepository::map).list();
    }

    public Optional<TodoItem> findItem(long id) {
        return jdbc.sql("SELECT * FROM todo_item WHERE id = ?").param(id).query(TodoRepository::map).optional();
    }

    public long insertItem(ItemData data, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO todo_item (list_id, title, notes, assignee_id, due_at, priority, repeat, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(data.listId(), data.title(), data.notes(), data.assigneeId(), Db.text(data.dueAt()),
                        data.priority().name(), Db.text(data.repeat()), Db.text(now))
                .update(keys);
        return keys.getKey().longValue();
    }

    public void updateItem(long id, ItemData data) {
        jdbc.sql("""
                UPDATE todo_item SET list_id = ?, title = ?, notes = ?, assignee_id = ?, due_at = ?, priority = ?,
                                     repeat = ?
                WHERE id = ?""")
                .params(data.listId(), data.title(), data.notes(), data.assigneeId(), Db.text(data.dueAt()),
                        data.priority().name(), Db.text(data.repeat()), id)
                .update();
    }

    public void setDone(long id, boolean done, LocalDateTime doneAt) {
        jdbc.sql("UPDATE todo_item SET done = ?, done_at = ? WHERE id = ?").params(done, Db.text(doneAt), id).update();
    }

    public void deleteItem(long id) {
        jdbc.sql("DELETE FROM todo_item WHERE id = ?").param(id).update();
    }

    private static TodoItem map(ResultSet rs, int n) throws SQLException {
        return new TodoItem(rs.getLong("id"), rs.getLong("list_id"), rs.getString("title"), rs.getString("notes"),
                Db.nullableLong(rs, "assignee_id"), Db.dateTime(rs, "due_at"),
                Priority.valueOf(rs.getString("priority")), Db.enumValue(rs, "repeat", Repeat.class),
                rs.getBoolean("done"), Db.dateTime(rs, "done_at"));
    }
}
