// Type of a planning entry; each type references the module that owns its details.
package io.github.alexbosch3000.familyhub.planning;

public enum EntryType {
    /** Appointment or activity, possibly with a cost forwarded to the budget. */
    EVENT,
    /** Shopping trip, linked to a shopping list. */
    SHOPPING,
    /** Planned meal, linked to a recipe and a number of servings. */
    MEAL,
    /** Budget session: producing a report or approving transactions. */
    ACCOUNTING
}
