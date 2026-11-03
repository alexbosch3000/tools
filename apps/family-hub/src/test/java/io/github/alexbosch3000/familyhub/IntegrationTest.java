// Base class of the HTTP-level tests: full application on a temporary SQLite file, fixed clock, MockMvc helpers.
package io.github.alexbosch3000.familyhub;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
public abstract class IntegrationTest {

    /** Tuesday 3 November 2026, 10:00 in Paris. */
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);
    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    @Autowired
    protected MockMvc mvc;

    /**
     * A fresh database file for the test context. Spring caches the context, so every test class
     * extending this one shares it: tests create their own data and never assume an empty database.
     */
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        try {
            Path dir = Files.createTempDirectory("family-hub-test");
            registry.add("app.data-dir", dir::toString);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Creates the first parent if needed and returns a session logged in as that parent. */
    protected MockHttpSession parentSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = mvc.perform(get("/api/session")).andReturn().getResponse().getContentAsString();
        if (JsonPath.<Boolean>read(state, "$.setupRequired")) {
            send(session, post("/api/setup"), """
                    {"name":"Alex","color":"#2563eb","username":"alex","password":"correct-horse"}""");
        } else {
            send(session, post("/api/auth/login"), """
                    {"username":"alex","password":"correct-horse"}""");
        }
        return session;
    }

    /** Creates a child with a login (once) and returns a session logged in as that child. */
    protected MockHttpSession childSession() throws Exception {
        MockHttpSession parent = parentSession();
        String members = mvc.perform(get("/api/members").session(parent)).andReturn().getResponse()
                .getContentAsString();
        if (JsonPath.<java.util.List<?>>read(members, "$[?(@.username == 'sam')]").isEmpty()) {
            send(parent, post("/api/members"), """
                    {"name":"Sam","color":"#16a34a","role":"CHILD","username":"sam","password":"child-password"}""");
        }
        MockHttpSession session = new MockHttpSession();
        send(session, post("/api/auth/login"), """
                {"username":"sam","password":"child-password"}""");
        return session;
    }

    protected ResultActions send(MockHttpSession session, MockHttpServletRequestBuilder request, String json)
            throws Exception {
        return mvc.perform(request.session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions getJson(MockHttpSession session, String url, Object... vars) throws Exception {
        return mvc.perform(get(url, vars).session(session));
    }

    protected ResultActions postJson(MockHttpSession session, String url, String json) throws Exception {
        return send(session, post(url), json);
    }

    protected ResultActions putJson(MockHttpSession session, String url, String json) throws Exception {
        return send(session, put(url), json);
    }

    protected ResultActions deleteJson(MockHttpSession session, String url) throws Exception {
        return mvc.perform(delete(url).session(session).with(csrf()));
    }

    /** Reads the numeric id of a JSON response body. */
    protected static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
