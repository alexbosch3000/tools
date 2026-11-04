// SQL access to recipes and their ingredients.
package io.github.alexbosch3000.familyhub.recipes.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.recipes.Recipe;
import io.github.alexbosch3000.familyhub.recipes.RecipeIngredient;
import io.github.alexbosch3000.familyhub.recipes.RecipeService.IngredientData;
import io.github.alexbosch3000.familyhub.recipes.RecipeService.RecipeData;
import io.github.alexbosch3000.familyhub.recipes.RecipeSummary;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RecipeRepository {

    private static final String SELECT_RECIPE = """
            SELECT id, name, servings, prep_minutes, cook_minutes, instructions, tags, source_url FROM recipe""";
    private static final String SELECT_INGREDIENT = """
            SELECT ri.recipe_id, ri.item_id, i.name AS item_name, ri.quantity, ri.unit, ri.note
            FROM recipe_ingredient ri JOIN item i ON i.id = ri.item_id""";

    private final JdbcClient jdbc;

    RecipeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<RecipeSummary> findSummaries(@Nullable String query) {
        String like = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        return jdbc.sql("""
                SELECT r.id, r.name, r.servings, r.prep_minutes, r.cook_minutes, r.tags,
                       (SELECT COUNT(*) FROM recipe_ingredient ri WHERE ri.recipe_id = r.id) AS ingredient_count
                FROM recipe r
                WHERE :like IS NULL OR r.name LIKE :like OR r.tags LIKE :like
                ORDER BY r.name COLLATE NOCASE""")
                .param("like", like)
                .query((rs, n) -> new RecipeSummary(rs.getLong("id"), rs.getString("name"), rs.getInt("servings"),
                        Db.nullableInt(rs, "prep_minutes"), Db.nullableInt(rs, "cook_minutes"),
                        Db.csv(rs, "tags"), rs.getInt("ingredient_count")))
                .list();
    }

    public Optional<Recipe> findById(long id) {
        List<RecipeIngredient> ingredients = jdbc
                .sql(SELECT_INGREDIENT + " WHERE ri.recipe_id = ? ORDER BY ri.position")
                .param(id).query(RecipeRepository::mapIngredient).list();
        return jdbc.sql(SELECT_RECIPE + " WHERE id = ?").param(id)
                .query((rs, n) -> mapRecipe(rs, ingredients)).optional();
    }

    public List<Recipe> findAll() {
        Map<Long, List<RecipeIngredient>> ingredients = new HashMap<>();
        jdbc.sql(SELECT_INGREDIENT + " ORDER BY ri.recipe_id, ri.position").query(rs -> {
            ingredients.computeIfAbsent(rs.getLong("recipe_id"), k -> new ArrayList<>()).add(mapIngredient(rs, 0));
        });
        return jdbc.sql(SELECT_RECIPE + " ORDER BY name COLLATE NOCASE")
                .query((rs, n) -> mapRecipe(rs, ingredients.getOrDefault(rs.getLong("id"), List.of())))
                .list();
    }

    public long insert(RecipeData data, LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO recipe (name, servings, prep_minutes, cook_minutes, instructions, tags, source_url,
                                    created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(data.name().trim(), data.servings(), data.prepMinutes(), data.cookMinutes(),
                        instructions(data), Db.csv(data.tags()), data.sourceUrl(), Db.text(now))
                .update(keys);
        return Db.key(keys);
    }

    public void update(long id, RecipeData data) {
        jdbc.sql("""
                UPDATE recipe SET name = ?, servings = ?, prep_minutes = ?, cook_minutes = ?, instructions = ?,
                                  tags = ?, source_url = ?
                WHERE id = ?""")
                .params(data.name().trim(), data.servings(), data.prepMinutes(), data.cookMinutes(),
                        instructions(data), Db.csv(data.tags()), data.sourceUrl(), id)
                .update();
    }

    public void replaceIngredients(long recipeId, List<IngredientData> ingredients) {
        jdbc.sql("DELETE FROM recipe_ingredient WHERE recipe_id = ?").param(recipeId).update();
        int position = 1;
        for (IngredientData ingredient : ingredients) {
            jdbc.sql("""
                    INSERT INTO recipe_ingredient (recipe_id, item_id, quantity, unit, note, position)
                    VALUES (?, ?, ?, ?, ?, ?)""")
                    .params(recipeId, ingredient.itemId(), ingredient.quantity().amount(),
                            ingredient.quantity().unit().name(), ingredient.note(), position++)
                    .update();
        }
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM recipe WHERE id = ?").param(id).update();
    }

    private static String instructions(RecipeData data) {
        return data.instructions() == null ? "" : data.instructions();
    }

    private static Recipe mapRecipe(ResultSet rs, List<RecipeIngredient> ingredients) throws SQLException {
        return new Recipe(rs.getLong("id"), rs.getString("name"), rs.getInt("servings"),
                Db.nullableInt(rs, "prep_minutes"), Db.nullableInt(rs, "cook_minutes"),
                rs.getString("instructions"), Db.csv(rs, "tags"), rs.getString("source_url"), ingredients);
    }

    private static RecipeIngredient mapIngredient(ResultSet rs, int row) throws SQLException {
        return new RecipeIngredient(rs.getLong("item_id"), rs.getString("item_name"),
                new Quantity(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit"))), rs.getString("note"));
    }
}
