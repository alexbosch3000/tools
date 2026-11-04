// Base class of the HTTP-level tests: full application on a temporary SQLite file, fixed clock, MockMvc helpers.
package io.github.alexbosch3000.familyhub;

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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "app.allowed-origins=https://alexbosch3000.github.io")
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

    /** Creates the first parent if needed and returns an access token of that parent. */
    protected String parentToken() throws Exception {
        String state = mvc.perform(get("/api/session")).andReturn().getResponse().getContentAsString();
        if (JsonPath.<Boolean>read(state, "$.setupRequired")) {
            return read(send(null, post("/api/setup"), """
                    {"name":"Alex","color":"#2563eb","username":"alex","password":"correct-horse"}"""), "$.token");
        }
        return login("alex", "correct-horse");
    }

    /** Creates a child with a login (once) and returns an access token of that child. */
    protected String childToken() throws Exception {
        String parent = parentToken();
        String members = getJson(parent, "/api/members").andReturn().getResponse().getContentAsString();
        if (JsonPath.<List<?>>read(members, "$[?(@.username == 'sam')]").isEmpty()) {
            send(parent, post("/api/members"), """
                    {"name":"Sam","color":"#16a34a","role":"CHILD","username":"sam","password":"child-password",
                     "clothingSize":"10 ans","shoeSize":"33"}""");
        }
        return login("sam", "child-password");
    }

    protected String login(String username, String password) throws Exception {
        return read(send(null, post("/api/auth/login"), """
                {"username":"%s","password":"%s"}""".formatted(username, password)), "$.token");
    }

    protected ResultActions send(String token, MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(authorized(request, token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions getJson(String token, String url, Object... vars) throws Exception {
        return mvc.perform(authorized(get(url, vars), token));
    }

    protected ResultActions postJson(String token, String url, String json) throws Exception {
        return send(token, post(url), json);
    }

    protected ResultActions putJson(String token, String url, String json) throws Exception {
        return send(token, put(url), json);
    }

    protected ResultActions deleteJson(String token, String url) throws Exception {
        return mvc.perform(authorized(delete(url), token));
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** Reads the numeric id of a JSON response body. */
    protected static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
