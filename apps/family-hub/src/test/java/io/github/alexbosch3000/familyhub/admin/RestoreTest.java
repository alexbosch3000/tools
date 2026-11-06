// Restore of a database backup by a parent: the data comes back, garbage and foreign roles are refused.
package io.github.alexbosch3000.familyhub.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RestoreTest extends IntegrationTest {

    private ResultActions upload(String token, String url, byte[] bytes) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_OCTET_STREAM).content(bytes);
        return mvc.perform(token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @Test
    void aBackupBringsBackTheDataItWasTakenWith() throws Exception {
        String token = parentToken();
        byte[] backup = getJson(token, "/api/admin/backup").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsByteArray();
        long listId = id(postJson(token, "/api/todo/lists", "{\"name\":\"Liste après sauvegarde\"}")
                .andExpect(status().isCreated()));

        upload(token, "/api/admin/restore", backup).andExpect(status().isNoContent());

        String again = login("alex", "correct-horse");
        List<Number> ids = read(getJson(again, "/api/todo/lists"), "$[?(@.id == " + listId + ")].id");
        assertThat(ids).isEmpty();
    }

    @Test
    void aFileThatIsNotABackupIsRefused() throws Exception {
        String token = parentToken();

        upload(token, "/api/admin/restore", "not a database".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
        getJson(token, "/api/todo/lists").andExpect(status().isOk());
    }

    @Test
    void aChildCannotRestore() throws Exception {
        upload(childToken(), "/api/admin/restore", new byte[] {1}).andExpect(status().isForbidden());
    }

    @Test
    void restoringWithoutSigningInIsRefusedOnceTheFamilyIsSetUp() throws Exception {
        parentToken();

        upload(null, "/api/setup/restore", new byte[] {1}).andExpect(status().isConflict());
        upload(null, "/api/admin/restore", new byte[] {1}).andExpect(status().isUnauthorized());
    }
}
