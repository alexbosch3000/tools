// Keeps the committed OpenAPI contract, from which the UI's TypeScript client is generated, in step with the API.
package io.github.alexbosch3000.familyhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

class OpenApiContractTest extends IntegrationTest {

    private static final Path CONTRACT = Path.of("web/src/api/openapi.json");

    /** `make api` runs this test with -DupdateContract=true to rewrite the contract after an API change. */
    @Test
    void committedContractDescribesTheApi() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        String actual = mapper.writeValueAsString(mapper.readTree(json)) + "\n";
        if (Boolean.getBoolean("updateContract")) {
            Files.writeString(CONTRACT, actual);
        }
        assertThat(Files.exists(CONTRACT)).as("missing %s: run `make api`", CONTRACT).isTrue();
        assertThat(Files.readString(CONTRACT)).as("the API changed: run `make api` and commit %s", CONTRACT)
                .isEqualTo(actual);
    }
}
