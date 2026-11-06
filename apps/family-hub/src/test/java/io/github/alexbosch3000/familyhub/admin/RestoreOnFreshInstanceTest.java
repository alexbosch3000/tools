// A fresh instance accepts a backup without signing in, which automates setting up a new server.
package io.github.alexbosch3000.familyhub.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbosch3000.familyhub.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.TestPropertySource;

// A distinct application name gives this class its own context, hence its own empty database.
@TestPropertySource(properties = "spring.application.name=family-hub-fresh")
class RestoreOnFreshInstanceTest extends IntegrationTest {

    private static byte[] backupWithAParent() throws Exception {
        Path file = Files.createTempFile("family-hub-source", ".db");
        String url = "jdbc:sqlite:" + file;
        Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration").load().migrate();
        try (Connection connection = DriverManager.getConnection(url);
                PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO member (name, color, role, username, password_hash, created_at)
                        VALUES ('Camille', '#2563eb', 'PARENT', 'camille', ?, '2026-11-03T10:00:00')""")) {
            insert.setString(1, PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("fresh-password"));
            insert.executeUpdate();
        }
        try {
            return Files.readAllBytes(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void aBackupSetsUpAFreshInstanceWithoutSigningIn() throws Exception {
        mvc.perform(get("/api/session")).andExpect(jsonPath("$.setupRequired").value(true));

        mvc.perform(post("/api/setup/restore").contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(backupWithAParent())).andExpect(status().isNoContent());

        mvc.perform(get("/api/session")).andExpect(jsonPath("$.setupRequired").value(false));
        login("camille", "fresh-password");
    }
}
