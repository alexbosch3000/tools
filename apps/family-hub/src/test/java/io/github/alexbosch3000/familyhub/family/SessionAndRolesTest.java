// Setup, login, CSRF and role-based access across the API.
package io.github.alexbosch3000.familyhub.family;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class SessionAndRolesTest extends IntegrationTest {

    @Test
    void setupCreatesTheFirstParentOnlyOnce() throws Exception {
        MockHttpSession parent = parentSession();
        getJson(parent, "/api/session")
                .andExpect(jsonPath("$.setupRequired").value(false))
                .andExpect(jsonPath("$.member.role").value("PARENT"))
                .andExpect(jsonPath("$.member.username").value("alex"));

        postJson(new MockHttpSession(), "/api/setup", """
                {"name":"Intruder","color":"#000000","username":"intruder","password":"whatever-123"}""")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        parentSession();
        postJson(new MockHttpSession(), "/api/auth/login", """
                {"username":"alex","password":"wrong-password"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Identifiant ou mot de passe incorrect."));
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/recipes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/todo/lists").session(parentSession())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void childrenCannotReachAccountingNorManageMembers() throws Exception {
        MockHttpSession child = childSession();
        getJson(child, "/api/recipes").andExpect(status().isOk());
        getJson(child, "/api/accounting/categories").andExpect(status().isForbidden());
        getJson(child, "/api/members").andExpect(status().isOk());
        postJson(child, "/api/members", """
                {"name":"X","color":"#000000","role":"PARENT"}""").andExpect(status().isForbidden());
    }

    @Test
    void parentCannotDeleteItself() throws Exception {
        MockHttpSession parent = parentSession();
        long me = ((Number) read(getJson(parent, "/api/session"), "$.member.id")).longValue();
        deleteJson(parent, "/api/members/" + me).andExpect(status().isBadRequest());
    }

    @Test
    void servesSecurityHeadersAndHealth() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk());
        mvc.perform(get("/readyz")).andExpect(status().isOk());
        mvc.perform(get("/api/session"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
