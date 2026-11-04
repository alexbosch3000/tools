// Public API of the to-do module: lists and items, with repeating chores.
package io.github.alexbosch3000.familyhub.todo;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import io.github.alexbosch3000.familyhub.todo.internal.TodoMetrics;
import io.github.alexbosch3000.familyhub.todo.internal.TodoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

    private final TodoRepository repository;
    private final MemberService members;
    private final TodoMetrics metrics;
    private final Clock clock;

    TodoService(TodoRepository repository, MemberService members, TodoMetrics metrics, Clock clock) {
        this.repository = repository;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record TodoItemData(long listId, String title, @Nullable String notes, @Nullable Long assigneeId,
            @Nullable LocalDateTime dueAt, @Nullable Priority priority, @Nullable Repeat repeat) {
    }

    public List<TodoList> lists() {
        return repository.findLists();
    }

    @Transactional
    public TodoList createList(String name) {
        ensureListNameFree(name, null);
        long id = repository.insertList(name.trim(), LocalDateTime.now(clock));
        return list(id);
    }

    @Transactional
    public TodoList renameList(long id, String name) {
        list(id);
        ensureListNameFree(name, id);
        repository.renameList(id, name.trim());
        return list(id);
    }

    @Transactional
    public void deleteList(long id) {
        list(id);
        repository.deleteList(id);
    }

    /** Items, optionally filtered; open items first by due date, then the most recently done. */
    public List<TodoItem> items(@Nullable Long listId, @Nullable Long assigneeId, boolean includeDone) {
        return repository.findItems(listId, assigneeId, includeDone);
    }

    public List<TodoItem> openItems() {
        return repository.findItems(null, null, false);
    }

    public TodoItem get(long id) {
        return repository.findItem(id).orElseThrow(() -> new NotFoundException("Tâche introuvable."));
    }

    @Transactional
    public TodoItem create(TodoItemData data) {
        return get(repository.insertItem(validated(data), LocalDateTime.now(clock)));
    }

    @Transactional
    public TodoItem update(long id, TodoItemData data) {
        get(id);
        repository.updateItem(id, validated(data));
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        get(id);
        repository.deleteItem(id);
    }

    /** Completing a repeating item with a due date creates its next occurrence. */
    @Transactional
    public TodoItem setDone(long id, boolean done) {
        TodoItem item = get(id);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        repository.setDone(id, done, done ? now : null);
        if (done && !item.done()) {
            metrics.completed();
        }
        if (done && !item.done() && item.repeat() != null && item.dueAt() != null) {
            repository.insertItem(new TodoItemData(item.listId(), item.title(), item.notes(), item.assigneeId(),
                    item.repeat().next(item.dueAt(), now), item.priority(), item.repeat()), now);
        }
        return get(id);
    }

    private TodoList list(long id) {
        return repository.findLists().stream().filter(l -> l.id() == id).findFirst()
                .orElseThrow(() -> new NotFoundException("Liste introuvable."));
    }

    private TodoItemData validated(TodoItemData data) {
        list(data.listId());
        if (data.assigneeId() != null && members.list().stream().map(Member::id)
                .noneMatch(id -> id.equals(data.assigneeId()))) {
            throw new InvalidRequestException("Membre inconnu.");
        }
        if (data.repeat() != null && data.dueAt() == null) {
            throw new InvalidRequestException("Une tâche répétée a besoin d'une échéance.");
        }
        LocalDateTime due = data.dueAt() == null ? null : data.dueAt().truncatedTo(ChronoUnit.MINUTES);
        return new TodoItemData(data.listId(), data.title().trim(), data.notes() == null ? "" : data.notes(),
                data.assigneeId(), due, data.priority() == null ? Priority.NORMAL : data.priority(), data.repeat());
    }

    private void ensureListNameFree(String name, @Nullable Long exceptId) {
        if (repository.listNameTaken(name.trim(), exceptId)) {
            throw new ConflictException("Une liste porte déjà ce nom.");
        }
    }
}
