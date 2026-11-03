// Raised when the current state forbids an operation (duplicate, resource in use); answered with HTTP 409.
package io.github.alexbosch3000.familyhub.shared;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
