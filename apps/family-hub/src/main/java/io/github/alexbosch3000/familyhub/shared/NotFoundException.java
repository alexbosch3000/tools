// Raised when a requested resource does not exist; answered with HTTP 404.
package io.github.alexbosch3000.familyhub.shared;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
