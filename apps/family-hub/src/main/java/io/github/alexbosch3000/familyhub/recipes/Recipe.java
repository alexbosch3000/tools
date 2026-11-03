// A recipe with its ingredients, for a given number of servings.
package io.github.alexbosch3000.familyhub.recipes;

import java.util.List;

public record Recipe(long id, String name, int servings, Integer prepMinutes, Integer cookMinutes,
        String instructions, List<String> tags, String sourceUrl, List<RecipeIngredient> ingredients) {

    /** Same recipe with every quantity adjusted to another number of servings. */
    public Recipe scaledTo(int targetServings) {
        double ratio = (double) targetServings / servings;
        List<RecipeIngredient> scaled = ingredients.stream()
                .map(i -> new RecipeIngredient(i.itemId(), i.itemName(), i.quantity().scaled(ratio), i.note()))
                .toList();
        return new Recipe(id, name, targetServings, prepMinutes, cookMinutes, instructions, tags, sourceUrl, scaled);
    }
}
