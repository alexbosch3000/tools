// A family member as exposed to other modules and to the API (never carries credentials).
package io.github.alexbosch3000.familyhub.family;

import org.jspecify.annotations.Nullable;

/**
 * @param username        null when the member cannot log in (a young child, for instance)
 * @param clothingSize    free text such as "8 ans" or "M", shown when buying clothes
 * @param shoeSize        free text such as "32"
 * @param hasCalendarFeed whether a calendar subscription token is active
 */
public record Member(long id, String name, String color, Role role, @Nullable String username,
        @Nullable String clothingSize, @Nullable String shoeSize, boolean hasCalendarFeed) {
}
