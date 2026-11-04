// Recipe as shown in lists, without instructions.
package io.github.alexbosch3000.familyhub.recipes;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record RecipeSummary(long id, String name, int servings, @Nullable Integer prepMinutes,
        @Nullable Integer cookMinutes, List<String> tags, int ingredientCount) {
}
