// Setup, token login and logout, login throttling, CORS and role-based access across the API.
package io.github.alexbosch3000.familyhub.family;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class SessionAndRolesTest extends IntegrationTest {

    @Test
    void setupCreatesTheFirstParentOnlyOnce() throws Exception {
        String parent = parentToken();
        getJson(parent, "/api/session")
                .andExpect(jsonPath("$.setupRequired").value(false))
                .andExpect(jsonPath("$.member.role").value("PARENT"))
                .andExpect(jsonPath("$.member.username").value("alex"));

        postJson(null, "/api/setup", """
                {"name":"Intruder","color":"#000000","username":"intruder","password":"whatever-123"}""")
                .andExpect(status().isConflict());
    }

    @Test
    void issuesPrefixedTokensThatLogoutRevokes() throws Exception {
        parentToken();
        String token = login("alex", "correct-horse");
        org.assertj.core.api.Assertions.assertThat(token).startsWith("fh_").hasSizeGreaterThan(40);
        getJson(token, "/api/recipes").andExpect(status().isOk());

        postJson(token, "/api/auth/logout", "{}").andExpect(status().isNoContent());

        getJson(token, "/api/recipes").andExpect(status().isUnauthorized());
        getJson(token, "/api/session").andExpect(jsonPath("$.member").doesNotExist());
    }

    @Test
    void rejectsWrongPasswordsThenThrottlesTheUsername() throws Exception {
        String parent = parentToken();
        postJson(parent, "/api/members", """
                {"name":"Guess","color":"#000000","role":"CHILD","username":"guess","password":"right-password"}""");
        for (int i = 0; i < 5; i++) {
            postJson(null, "/api/auth/login", """
                    {"username":"guess","password":"wrong-password"}""")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Identifiant ou mot de passe incorrect."));
        }
        postJson(null, "/api/auth/login", """
                {"username":"guess","password":"right-password"}""")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recipes").header(HttpHeaders.AUTHORIZATION, "Bearer fh_unknown"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsOnlyTheConfiguredUserInterfaceOrigins() throws Exception {
        mvc.perform(options("/api/recipes")
                        .header(HttpHeaders.ORIGIN, "https://alexbosch3000.github.io")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://alexbosch3000.github.io"));
        mvc.perform(options("/api/recipes")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void childrenCannotReachAccountingNorManageMembers() throws Exception {
        String child = childToken();
        getJson(child, "/api/recipes").andExpect(status().isOk());
        getJson(child, "/api/accounting/categories").andExpect(status().isForbidden());
        getJson(child, "/api/members").andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'sam')].shoeSize").value("33"));
        postJson(child, "/api/members", """
                {"name":"X","color":"#000000","role":"PARENT"}""").andExpect(status().isForbidden());
    }

    @Test
    void changingAPasswordSignsTheMemberOutEverywhere() throws Exception {
        String parent = parentToken();
        long id = id(postJson(parent, "/api/members", """
                {"name":"Lou","color":"#000000","role":"CHILD","username":"lou","password":"first-password"}"""));
        String lou = login("lou", "first-password");

        putJson(parent, "/api/members/" + id, """
                {"name":"Lou","color":"#000000","role":"CHILD","username":"lou","password":"second-password"}""")
                .andExpect(status().isOk());

        getJson(lou, "/api/recipes").andExpect(status().isUnauthorized());
    }

    @Test
    void parentCannotDeleteItself() throws Exception {
        String parent = parentToken();
        long me = ((Number) read(getJson(parent, "/api/session"), "$.member.id")).longValue();
        deleteJson(parent, "/api/members/" + me).andExpect(status().isBadRequest());
    }

    @Test
    void servesSecurityHeadersHealthAndNoSessionCookie() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk());
        mvc.perform(get("/readyz")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"alex\",\"password\":\"correct-horse\"}"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        mvc.perform(get("/api/session"))
                .andExpect(header().string("Content-Security-Policy", Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
