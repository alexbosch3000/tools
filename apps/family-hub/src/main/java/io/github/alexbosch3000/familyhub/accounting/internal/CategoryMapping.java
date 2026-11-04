// Maps the categories of other modules onto budget categories (the anti-corruption layer of accounting).
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.planning.EventCategory;
import org.jspecify.annotations.Nullable;

final class CategoryMapping {

    static final String FALLBACK = "divers";

    private CategoryMapping() {
    }

    static String forEvent(@Nullable EventCategory category) {
        if (category == null) {
            return FALLBACK;
        }
        return switch (category) {
            case VACATION -> "vacances";
            case DAY_CAMP -> "garde";
            case TAXES -> "impots";
            case SPORT -> "sport";
            case HEALTH -> "sante";
            case SCHOOL -> "scolarite";
            case LEISURE -> "loisirs";
            case BIRTHDAY -> "cadeaux";
            case HOUSEHOLD -> "maison";
            case UTILITIES -> "energie";
            case OTHER -> FALLBACK;
        };
    }

    static String forStore(StoreType storeType) {
        return switch (storeType) {
            case SUPERMARKET, MARKET, BAKERY, BUTCHER -> "courses";
            case PHARMACY -> "sante";
            case CLOTHING -> "vetements";
            case DIY, GARDEN -> "maison";
            case ONLINE, OTHER -> FALLBACK;
        };
    }
}
