// Raised when a request breaks a business rule that annotations cannot express; answered with HTTP 400.
package io.github.alexbosch3000.familyhub.shared;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
