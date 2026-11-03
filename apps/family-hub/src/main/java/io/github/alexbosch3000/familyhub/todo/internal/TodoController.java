// To-do API: lists and items.
package io.github.alexbosch3000.familyhub.todo.internal;

import io.github.alexbosch3000.familyhub.todo.Priority;
import io.github.alexbosch3000.familyhub.todo.Repeat;
import io.github.alexbosch3000.familyhub.todo.TodoItem;
import io.github.alexbosch3000.familyhub.todo.TodoList;
import io.github.alexbosch3000.familyhub.todo.TodoService;
import io.github.alexbosch3000.familyhub.todo.TodoService.ItemData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
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
@RequestMapping("/api/todo")
class TodoController {

    private final TodoService todos;

    TodoController(TodoService todos) {
        this.todos = todos;
    }

    record ListRequest(@NotBlank @Size(max = 60) String name) {
    }

    record ItemRequest(@NotNull Long listId, @NotBlank @Size(max = 200) String title, @Size(max = 4000) String notes,
            Long assigneeId, LocalDateTime dueAt, Priority priority, Repeat repeat) {

        ItemData toData() {
            return new ItemData(listId, title, notes, assigneeId, dueAt, priority, repeat);
        }
    }

    record DoneRequest(boolean done) {
    }

    @GetMapping("/lists")
    List<TodoList> lists() {
        return todos.lists();
    }

    @PostMapping("/lists")
    @ResponseStatus(HttpStatus.CREATED)
    TodoList createList(@Valid @RequestBody ListRequest request) {
        return todos.createList(request.name());
    }

    @PutMapping("/lists/{id}")
    TodoList renameList(@PathVariable long id, @Valid @RequestBody ListRequest request) {
        return todos.renameList(id, request.name());
    }

    @DeleteMapping("/lists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteList(@PathVariable long id) {
        todos.deleteList(id);
    }

    @GetMapping("/items")
    List<TodoItem> items(@RequestParam(required = false) Long listId, @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "false") boolean includeDone) {
        return todos.items(listId, assigneeId, includeDone);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    TodoItem create(@Valid @RequestBody ItemRequest request) {
        return todos.create(request.toData());
    }

    @PutMapping("/items/{id}")
    TodoItem update(@PathVariable long id, @Valid @RequestBody ItemRequest request) {
        return todos.update(id, request.toData());
    }

    @PutMapping("/items/{id}/done")
    TodoItem done(@PathVariable long id, @RequestBody DoneRequest request) {
        return todos.setDone(id, request.done());
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        todos.delete(id);
    }
}
