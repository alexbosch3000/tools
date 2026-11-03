// A family member as exposed to other modules and to the API (never carries credentials).
package io.github.alexbosch3000.familyhub.family;

/**
 * @param username null when the member cannot log in (a young child, for instance)
 * @param hasCalendarFeed whether a calendar subscription token is active
 */
public record Member(long id, String name, String color, Role role, String username, boolean hasCalendarFeed) {
}
