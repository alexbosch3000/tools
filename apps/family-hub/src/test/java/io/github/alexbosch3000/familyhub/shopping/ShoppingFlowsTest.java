// HTTP-level flows of the shopping module: lists, rows, recipes.
package io.github.alexbosch3000.familyhub.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexbosch3000.familyhub.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShoppingFlowsTest extends IntegrationTest {

    @Test
    void renamesAListAndMovesItsPlannedDate() throws Exception {
        String token = parentToken();
        long id = id(postJson(token, "/api/shopping/lists", """
                {"name":"Samedi","plannedFor":"2026-11-07"}""").andExpect(status().isCreated()));

        putJson(token, "/api/shopping/lists/" + id, """
                {"name":"Marché du dimanche","plannedFor":"2026-11-08"}""").andExpect(status().isOk());

        getJson(token, "/api/shopping/lists/" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Marché du dimanche"))
                .andExpect(jsonPath("$.plannedFor").value("2026-11-08"));
    }

    private long catalogId(String token, String path, String name) throws Exception {
        List<Number> ids = read(getJson(token, path + "?q={q}", name), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    @Test
    void skippedIngredientsAreLeftOutOfTheList() throws Exception {
        String token = parentToken();
        long crepes = catalogId(token, "/api/recipes", "Crêpes");
        long flour = catalogId(token, "/api/items", "Farine");
        long listId = id(postJson(token, "/api/shopping/lists", """
                {"name":"Crêpes sans farine"}""").andExpect(status().isCreated()));

        postJson(token, "/api/shopping/lists/" + listId + "/recipes", """
                {"recipeId":%d,"servings":4,"skippedItemIds":[%d]}""".formatted(crepes, flour))
                .andExpect(status().isOk());

        String list = getJson(token, "/api/shopping/lists/" + listId).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<?>>read(list, "$.rows[?(@.name == 'Farine')]")).isEmpty();
        assertThat(JsonPath.<List<?>>read(list, "$.rows[?(@.name == 'Beurre')]")).isNotEmpty();
        assertThat(JsonPath.<List<String>>read(list, "$.recipes[*].name")).contains("Crêpes");
    }

    private static String itemPath(long listId, long itemId) {
        return "/api/shopping/lists/" + listId + "/items/" + itemId + "/purchase";
    }

    private String list(String token, long listId) throws Exception {
        return getJson(token, "/api/shopping/lists/" + listId).andReturn().getResponse().getContentAsString();
    }

    @Test
    void aPurchaseOverridesWhatIsNeededAndGoesBackToTheComputation() throws Exception {
        String token = parentToken();
        long salt = catalogId(token, "/api/items", "Sel");
        long listId = id(postJson(token, "/api/shopping/lists", """
                {"name":"Sel acheté en paquet"}""").andExpect(status().isCreated()));
        postJson(token, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":%d,"quantity":4,"unit":"PINCH"}""".formatted(salt)).andExpect(status().isOk());
        int unpricedBefore = JsonPath.read(list(token, listId), "$.unpricedRows");
        assertThat(unpricedBefore).isPositive();

        putJson(token, itemPath(listId, salt), """
                {"quantity":1,"unit":"PACK","priceCents":120,"note":"  gros sel  "}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].purchase.quantity.unit").value("PACK"))
                .andExpect(jsonPath("$.rows[0].purchase.note").value("gros sel"))
                .andExpect(jsonPath("$.rows[0].estimatedCents").value(120))
                .andExpect(jsonPath("$.unpricedRows").value(unpricedBefore - 1));

        deleteJson(token, itemPath(listId, salt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].purchase").doesNotExist())
                .andExpect(jsonPath("$.unpricedRows").value(unpricedBefore));
    }

    @Test
    void aPurchaseDoesNotSurviveItsRow() throws Exception {
        String token = parentToken();
        long salt = catalogId(token, "/api/items", "Sel");
        long listId = id(postJson(token, "/api/shopping/lists", """
                {"name":"Sel retiré"}""").andExpect(status().isCreated()));
        postJson(token, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":%d,"quantity":4,"unit":"PINCH"}""".formatted(salt)).andExpect(status().isOk());
        putJson(token, itemPath(listId, salt), """
                {"quantity":1,"unit":"PACK","priceCents":120}""").andExpect(status().isOk());

        deleteJson(token, "/api/shopping/lists/" + listId + "/items/" + salt).andExpect(status().isOk());
        postJson(token, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":%d,"quantity":4,"unit":"PINCH"}""".formatted(salt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].purchase").doesNotExist());
    }

    @Test
    void aPurchaseOnARowOfARemovedRecipeIsDropped() throws Exception {
        String token = parentToken();
        long crepes = catalogId(token, "/api/recipes", "Crêpes");
        long butter = catalogId(token, "/api/items", "Beurre");
        long listId = id(postJson(token, "/api/shopping/lists", """
                {"name":"Crêpes puis rien"}""").andExpect(status().isCreated()));
        postJson(token, "/api/shopping/lists/" + listId + "/recipes", """
                {"recipeId":%d,"servings":4}""".formatted(crepes)).andExpect(status().isOk());
        putJson(token, itemPath(listId, butter), """
                {"quantity":1,"unit":"PACK","priceCents":250}""").andExpect(status().isOk());
        long entryId = JsonPath.<Number>read(list(token, listId), "$.recipes[0].entryId").longValue();

        deleteJson(token, "/api/shopping/lists/" + listId + "/recipes/" + entryId).andExpect(status().isOk());
        postJson(token, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":%d,"quantity":100,"unit":"G"}""".formatted(butter)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].purchase").doesNotExist());
    }

    @Test
    void aPurchaseNeedsARowOnTheList() throws Exception {
        String token = parentToken();
        long salt = catalogId(token, "/api/items", "Sel");
        long listId = id(postJson(token, "/api/shopping/lists", """
                {"name":"Liste vide"}""").andExpect(status().isCreated()));

        putJson(token, itemPath(listId, salt), """
                {"quantity":1,"unit":"PACK"}""").andExpect(status().isNotFound());
    }
}
