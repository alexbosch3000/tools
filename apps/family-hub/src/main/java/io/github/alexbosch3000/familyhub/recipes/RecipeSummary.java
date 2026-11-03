// Recipe as shown in lists, without instructions.
package io.github.alexbosch3000.familyhub.recipes;

import java.util.List;

public record RecipeSummary(long id, String name, int servings, Integer prepMinutes, Integer cookMinutes,
        List<String> tags, int ingredientCount) {
}
