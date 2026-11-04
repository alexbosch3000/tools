// HTTP-level flows of the shopping module: lists, rows, recipes.
package io.github.alexbosch3000.familyhub.shopping;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import org.junit.jupiter.api.Test;

class ShoppingFlowsTest extends IntegrationTest {

    @Test
    void renamesAListAndMovesItsPlannedDate() throws Exception {
        String token = parentToken();
        long id = id(postJson(token, "/api/shopping/lists", """
                {"name":"Samedi","plannedFor":"2026-11-07"}""").andExpect(status().isOk()));

        putJson(token, "/api/shopping/lists/" + id, """
                {"name":"Marché du dimanche","plannedFor":"2026-11-08"}""").andExpect(status().isOk());

        getJson(token, "/api/shopping/lists/" + id).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Marché du dimanche"))
                .andExpect(jsonPath("$.plannedFor").value("2026-11-08"));
    }
}
