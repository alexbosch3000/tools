// End-to-end flows across modules: meals to shopping, shopping to budget, planning costs, recurring bills, feeds.
package io.github.alexbosch3000.familyhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class CrossModuleFlowsTest extends IntegrationTest {

    private long recipeId(String token, String name) throws Exception {
        List<Number> ids = read(getJson(token, "/api/recipes?q={q}", name), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    private long itemId(String token, String name) throws Exception {
        List<Number> ids = read(getJson(token, "/api/items?q={q}", name), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    @Test
    void plannedMealsBecomeAScaledShoppingList() throws Exception {
        String parent = parentToken();
        long carbonara = recipeId(parent, "Pâtes carbonara");
        long crepes = recipeId(parent, "Crêpes");
        postJson(parent, "/api/planning/entries", """
                {"title":"Carbonara","type":"MEAL","start":"2026-11-04T19:30","end":"2026-11-04T20:30",
                 "recipeId":%d,"servings":6}""".formatted(carbonara)).andExpect(status().isCreated());
        postJson(parent, "/api/planning/entries", """
                {"title":"Crêpes","type":"MEAL","start":"2026-11-06T16:30","end":"2026-11-06T17:00",
                 "recipeId":%d}""".formatted(crepes))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servings").value(4));

        long listId = ((Number) read(postJson(parent, "/api/planning/meals/shopping",
                "{\"from\":\"2026-11-02\",\"to\":\"2026-11-08\"}").andExpect(jsonPath("$.meals").value(2)),
                "$.shoppingListId")).longValue();

        String list = getJson(parent, "/api/shopping/lists/{id}", listId).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Courses du 02/11 au 08/11"))
                .andExpect(jsonPath("$.recipes.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> pasta = JsonPath.<List<Map<String, Object>>>read(list, "$.rows[?(@.name == 'Pâtes')]")
                .getFirst();
        assertThat(pasta).containsEntry("quantities", List.of(Map.of("amount", 600.0, "unit", "G")));
        Map<String, Object> eggs = JsonPath.<List<Map<String, Object>>>read(list, "$.rows[?(@.name == 'Œufs')]")
                .getFirst();
        assertThat(eggs).containsEntry("quantities", List.of(Map.of("amount", 10.0, "unit", "PIECE")));
        assertThat(eggs.get("recipes")).asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder("Pâtes carbonara", "Crêpes");
        assertThat(((Number) JsonPath.read(list, "$.estimatedTotalCents")).longValue()).isPositive();
    }

    @Test
    void completingAShoppingListRecordsAnExpenseToApproveAndClosesItsPlanningEntry() throws Exception {
        String parent = parentToken();
        long entryId = id(postJson(parent, "/api/planning/entries", """
                {"title":"Courses du samedi","type":"SHOPPING","start":"2026-11-07T10:00","end":"2026-11-07T11:00",
                 "createShoppingList":true}""").andExpect(status().isCreated()));
        long listId = ((Number) read(getJson(parent, "/api/planning/entries/{id}", entryId), "$.shoppingListId"))
                .longValue();
        postJson(parent, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":%d,"quantity":2,"unit":"L"}""".formatted(itemId(parent, "Lait"))).andExpect(status().isOk());

        putJson(parent, "/api/shopping/lists/" + listId + "/items/" + itemId(parent, "Lait") + "/checked",
                "{\"checked\":true}").andExpect(jsonPath("$.checkedCount").value(1));
        postJson(parent, "/api/shopping/lists/" + listId + "/complete", """
                {"payments":[{"storeType":"SUPERMARKET","amountCents":8740}],"paidOn":"2026-11-07"}""")
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.amountPaidCents").value(8740));

        getJson(parent, "/api/planning/entries/{id}", entryId).andExpect(jsonPath("$.done").value(true));
        String approvals = getJson(parent, "/api/accounting/approvals").andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> fromShopping = JsonPath.read(approvals,
                "$[?(@.sourceRef == 'list-" + listId + "-SUPERMARKET')]");
        assertThat(fromShopping).hasSize(1);
        assertThat(fromShopping.getFirst()).containsEntry("amountCents", 8740).containsEntry("categoryCode", "courses")
                .containsEntry("status", "PENDING");

        long transactionId = ((Number) fromShopping.getFirst().get("id")).longValue();
        postJson(parent, "/api/accounting/transactions/" + transactionId + "/approve", "{\"amountCents\":8790}")
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").isNumber());
        getJson(parent, "/api/accounting/reports/monthly?month=2026-11")
                .andExpect(jsonPath("$.categories[?(@.code == 'courses')].actualCents").value(8790))
                .andExpect(jsonPath("$.netActualCents").isNumber())
                .andExpect(jsonPath("$.categories[0].remainingCents").hasJsonPath());
        postJson(parent, "/api/shopping/lists/" + listId + "/lines", """
                {"itemId":1,"quantity":1,"unit":"KG"}""").andExpect(status().isConflict());
    }

    @Test
    void anEventCostIsPlannedInTheBudgetAndFollowsTheEntry() throws Exception {
        String parent = parentToken();
        String vacation = """
                {"title":"Vacances au ski","type":"EVENT","category":"VACATION","allDay":true,
                 "start":"2027-02-13T00:00","end":"2027-02-20T00:00","estimatedCostCents":%d}""";
        long entryId = id(postJson(parent, "/api/planning/entries", vacation.formatted(180_000))
                .andExpect(jsonPath("$.end").value("2027-02-20T23:59:00")));
        getJson(parent, "/api/accounting/transactions?month=2027-02")
                .andExpect(jsonPath("$[0].status").value("PLANNED"))
                .andExpect(jsonPath("$[0].categoryCode").value("vacances"))
                .andExpect(jsonPath("$[0].amountCents").value(180_000))
                .andExpect(jsonPath("$[0].day").value("2027-02-13"));

        putJson(parent, "/api/planning/entries/" + entryId, vacation.formatted(210_000)).andExpect(status().isOk());
        getJson(parent, "/api/accounting/transactions?month=2027-02")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amountCents").value(210_000));

        deleteJson(parent, "/api/planning/entries/" + entryId).andExpect(status().isNoContent());
        getJson(parent, "/api/accounting/transactions?month=2027-02").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recurringBillsArePlannedForTheCurrentAndNextMonth() throws Exception {
        String parent = parentToken();
        postJson(parent, "/api/accounting/recurring", """
                {"label":"Électricité","categoryCode":"energie","supplier":"EDF","amountCents":9500,
                 "period":"MONTHLY","dayOfMonth":10,"startDate":"2026-01-10","active":true}""")
                .andExpect(status().isCreated());

        getJson(parent, "/api/accounting/transactions?month=2026-11")
                .andExpect(jsonPath("$[?(@.supplier == 'EDF')].day").value("2026-11-10"));
        getJson(parent, "/api/accounting/transactions?month=2026-12")
                .andExpect(jsonPath("$[?(@.supplier == 'EDF')].status").value("PLANNED"));
        postJson(parent, "/api/accounting/recurring/generate?month=2026-11", "{}")
                .andExpect(jsonPath("$.created").value(0));
        getJson(parent, "/api/accounting/reports/monthly?month=2026-11")
                .andExpect(jsonPath("$.categories[?(@.code == 'energie')].forecastCents").value(9500));
    }

    @Test
    void recurringEntriesExpandAndCanSkipAnOccurrence() throws Exception {
        String parent = parentToken();
        long me = ((Number) read(getJson(parent, "/api/session"), "$.member.id")).longValue();
        long judo = id(postJson(parent, "/api/planning/entries", """
                {"title":"Judo","type":"EVENT","category":"SPORT","start":"2026-11-04T17:00","end":"2026-11-04T18:00",
                 "memberIds":[%d],"recurrence":{"frequency":"WEEKLY","interval":1,"days":["WEDNESDAY"]}}"""
                .formatted(me)));
        postJson(parent, "/api/planning/entries/" + judo + "/exceptions", "{\"day\":\"2026-11-11\"}")
                .andExpect(status().isOk());

        getJson(parent, "/api/planning/occurrences?from=2026-11-01&to=2026-11-30")
                .andExpect(jsonPath("$[?(@.entry.title == 'Judo')].start", Matchers.contains(
                        "2026-11-04T17:00:00", "2026-11-18T17:00:00", "2026-11-25T17:00:00")));
        getJson(parent, "/api/dashboard")
                .andExpect(jsonPath("$.load[?(@.memberId == " + me + ")].entries").value(1))
                .andExpect(jsonPath("$.accounting").isMap());
    }

    @Test
    void validatesTypeSpecificFields() throws Exception {
        String parent = parentToken();
        postJson(parent, "/api/planning/entries", """
                {"title":"Repas","type":"MEAL","start":"2026-11-04T19:00","end":"2026-11-04T20:00"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Choisissez une recette pour ce repas."));
        postJson(parent, "/api/planning/entries", """
                {"title":"Envers","type":"EVENT","start":"2026-11-04T19:00","end":"2026-11-04T18:00"}""")
                .andExpect(status().isBadRequest());
        postJson(parent, "/api/planning/entries", """
                {"title":"","type":"EVENT","start":"2026-11-04T19:00","end":"2026-11-04T20:00"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void suggestsRecipesFromTheItemsOfAList() throws Exception {
        String parent = parentToken();
        String ids = itemId(parent, "Farine") + "," + itemId(parent, "Œufs") + "," + itemId(parent, "Lait");
        getJson(parent, "/api/recipes/suggestions?itemIds=" + ids)
                .andExpect(jsonPath("$[0].name").value("Crêpes"))
                .andExpect(jsonPath("$[0].missing", Matchers.containsInAnyOrder("Sucre", "Beurre")));
    }

    @Test
    void repeatingTodoCreatesItsNextOccurrenceWhenDone() throws Exception {
        String child = childToken();
        long listId = ((Number) ((List<?>) read(getJson(child, "/api/todo/lists"), "$[?(@.name == 'Maison')].id"))
                .getFirst()).longValue();
        long itemId = id(postJson(child, "/api/todo/items", """
                {"listId":%d,"title":"Sortir les poubelles","dueAt":"2026-11-03T20:00","repeat":"WEEKLY"}"""
                .formatted(listId)));

        putJson(child, "/api/todo/items/" + itemId + "/done", "{\"done\":true}")
                .andExpect(jsonPath("$.done").value(true));

        getJson(child, "/api/todo/items?listId=" + listId)
                .andExpect(jsonPath("$[?(@.title == 'Sortir les poubelles')].dueAt").value("2026-11-10T20:00:00"));
        getJson(child, "/api/dashboard").andExpect(jsonPath("$.accounting").doesNotExist());
    }

    @Test
    void calendarFeedIsReachableWithItsTokenOnly() throws Exception {
        String parent = parentToken();
        postJson(parent, "/api/planning/entries", """
                {"title":"Dentiste","type":"EVENT","category":"HEALTH",
                 "start":"2026-11-05T09:15","end":"2026-11-05T09:45"}""");
        String token = read(postJson(parent, "/api/members/me/calendar-token", "{}"), "$.token");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/planning/feed/" + token + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(Matchers.containsString("SUMMARY:Dentiste")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/planning/feed/not-a-token.ics"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clothesAreBoughtPerMemberAndRecordedAsClothingExpenses() throws Exception {
        String parent = parentToken();
        childToken();
        long sam = ((Number) ((List<?>) read(getJson(parent, "/api/members"), "$[?(@.username == 'sam')].id"))
                .getFirst()).longValue();
        long me = ((Number) read(getJson(parent, "/api/session"), "$.member.id")).longValue();
        long listId = id(postJson(parent, "/api/shopping/lists", "{\"name\":\"Rentrée\"}"));
        String lines = "/api/shopping/lists/" + listId + "/lines";
        long shoes = itemId(parent, "Chaussures");
        postJson(parent, lines, "{\"itemId\":%d,\"memberId\":%d,\"quantity\":1,\"unit\":\"PIECE\"}"
                .formatted(shoes, sam));
        postJson(parent, lines, "{\"itemId\":%d,\"memberId\":%d,\"quantity\":1,\"unit\":\"PIECE\"}"
                .formatted(shoes, me));
        postJson(parent, lines, "{\"itemId\":%d,\"quantity\":1,\"unit\":\"L\"}".formatted(itemId(parent, "Lait")))
                .andExpect(jsonPath("$.rows[?(@.name == 'Chaussures')].memberId", Matchers.containsInAnyOrder(
                        (int) sam, (int) me)))
                .andExpect(jsonPath("$.rows[?(@.name == 'Chaussures')].storeType", Matchers.everyItem(
                        Matchers.is("CLOTHING"))));

        putJson(parent, "/api/shopping/lists/" + listId + "/items/" + shoes + "/checked?memberId=" + sam,
                "{\"checked\":true}").andExpect(jsonPath("$.checkedCount").value(1));
        postJson(parent, "/api/shopping/lists/" + listId + "/complete", """
                {"payments":[{"storeType":"CLOTHING","amountCents":9000},{"storeType":"SUPERMARKET","amountCents":220}],
                 "paidOn":"2026-11-03"}""")
                .andExpect(jsonPath("$.amountPaidCents").value(9220));

        String approvals = getJson(parent, "/api/accounting/approvals").andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Integer>>read(approvals,
                "$[?(@.sourceRef == 'list-" + listId + "-CLOTHING')].amountCents")).containsExactly(9000);
        assertThat(JsonPath.<List<String>>read(approvals,
                "$[?(@.sourceRef == 'list-" + listId + "-CLOTHING')].categoryCode")).containsExactly("vetements");
        assertThat(JsonPath.<List<String>>read(approvals,
                "$[?(@.sourceRef == 'list-" + listId + "-SUPERMARKET')].categoryCode")).containsExactly("courses");

        String metrics = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/actuator/prometheus")).andReturn().getResponse().getContentAsString();
        assertThat(metrics).contains("familyhub_shopping_trips_total")
                .containsPattern("familyhub_shopping_spent_euros_count\\{[^}]*store_type=\"CLOTHING\"")
                .contains("familyhub_accounting_approvals_pending");
    }

    @Test
    void parentsCanDownloadAConsistentBackup() throws Exception {
        byte[] backup = getJson(parentToken(), "/api/admin/backup").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(backup, 0, 15, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("SQLite format 3");
        getJson(childToken(), "/api/admin/backup").andExpect(status().isForbidden());
    }
}
