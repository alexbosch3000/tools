// Restore of a database backup: by a parent, or without signing in while the instance has no member yet.
package io.github.alexbosch3000.familyhub.admin.internal;

import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RestoreController {

    private final RestoreService restores;

    RestoreController(RestoreService restores) {
        this.restores = restores;
    }

    /** The body is the backup file itself, as downloaded from {@code /api/admin/backup}. */
    @PostMapping(path = "/api/admin/restore", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
    void restore(HttpServletRequest request) throws IOException {
        restores.restore(request.getInputStream());
    }

    /** Same as {@link #restore}, open to anyone as long as nobody has set the family up: automates a new instance. */
    @PostMapping(path = "/api/setup/restore", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
    void setupRestore(HttpServletRequest request) throws IOException {
        if (!restores.freshInstance()) {
            throw new ConflictException("La famille est déjà configurée : connectez-vous pour restaurer.");
        }
        restores.restore(request.getInputStream());
    }
}
