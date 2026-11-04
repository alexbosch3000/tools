// A to-do list with the number of items still open.
package io.github.alexbosch3000.familyhub.todo;

public record TodoList(long id, String name, int openCount) {
}
