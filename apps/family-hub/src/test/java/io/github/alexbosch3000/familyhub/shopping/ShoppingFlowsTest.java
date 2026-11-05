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
}
