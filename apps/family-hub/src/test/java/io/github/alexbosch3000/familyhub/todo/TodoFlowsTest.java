// HTTP-level flows of the to-do module: start dates, the scheduled window and repeating items with a start.
package io.github.alexbosch3000.familyhub.todo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import org.junit.jupiter.api.Test;

class TodoFlowsTest extends IntegrationTest {

    private long newList(String token, String name) throws Exception {
        return id(postJson(token, "/api/todo/lists", "{\"name\":\"%s\"}".formatted(name))
                .andExpect(status().isCreated()));
    }

    @Test
    void scheduledReturnsItemsWhoseSpanMeetsThePeriod() throws Exception {
        String token = parentToken();
        long listId = newList(token, "Travaux de décembre");
        long itemId = id(postJson(token, "/api/todo/items", """
                {"listId":%d,"title":"Peindre le couloir","startAt":"2026-12-02T09:00","dueAt":"2026-12-04T18:00",
                 "priority":"HIGH"}""".formatted(listId)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.startAt").value("2026-12-02T09:00:00")));

        getJson(token, "/api/todo/scheduled?from=2026-12-04&to=2026-12-04").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(itemId)).isNotEmpty());
        getJson(token, "/api/todo/scheduled?from=2026-12-03&to=2026-12-03").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(itemId)).isNotEmpty());
        getJson(token, "/api/todo/scheduled?from=2026-12-05&to=2026-12-06").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(itemId)).isEmpty());
        getJson(token, "/api/todo/scheduled?from=2026-11-25&to=2026-12-01").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(itemId)).isEmpty());
    }

    @Test
    void scheduledRejectsAnInvalidPeriod() throws Exception {
        String token = parentToken();

        getJson(token, "/api/todo/scheduled?from=2026-12-05&to=2026-12-04").andExpect(status().isBadRequest());
        getJson(token, "/api/todo/scheduled?from=2026-01-01&to=2027-06-01").andExpect(status().isBadRequest());
    }

    @Test
    void anItemCannotStartAfterItIsDue() throws Exception {
        String token = parentToken();
        long listId = newList(token, "Début tardif");

        postJson(token, "/api/todo/items", """
                {"listId":%d,"title":"Impossible","startAt":"2026-12-05T09:00","dueAt":"2026-12-04T18:00"}"""
                .formatted(listId)).andExpect(status().isBadRequest());
    }

    @Test
    void completingARepeatingItemShiftsBothItsStartAndItsDueDate() throws Exception {
        String token = parentToken();
        long listId = newList(token, "Chantier hebdomadaire");
        long itemId = id(postJson(token, "/api/todo/items", """
                {"listId":%d,"title":"Tondre","startAt":"2026-11-09T09:00","dueAt":"2026-11-10T18:00",
                 "repeat":"WEEKLY"}""".formatted(listId)).andExpect(status().isCreated()));

        putJson(token, "/api/todo/items/" + itemId + "/done", "{\"done\":true}").andExpect(status().isOk());

        getJson(token, "/api/todo/items?listId=" + listId).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value("2026-11-16T09:00:00"))
                .andExpect(jsonPath("$[0].dueAt").value("2026-11-17T18:00:00"));
    }
}
