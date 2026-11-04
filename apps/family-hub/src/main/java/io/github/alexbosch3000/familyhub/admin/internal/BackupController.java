// Database backup download for parents: a consistent copy taken while the service runs.
package io.github.alexbosch3000.familyhub.admin.internal;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BackupController {

    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final JdbcClient jdbc;
    private final Clock clock;

    BackupController(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * {@code VACUUM INTO} writes a compacted, transactionally consistent copy of the database, which a
     * plain file copy of a live SQLite database in WAL mode does not guarantee.
     */
    @GetMapping("/api/admin/backup")
    @WithSpan
    ResponseEntity<byte[]> backup() throws IOException {
        Path dir = Files.createTempDirectory("family-hub-backup");
        Path file = dir.resolve("backup.db");
        try {
            jdbc.sql("VACUUM INTO ?").param(file.toString()).update();
            String name = "family-hub-" + STAMP.format(LocalDateTime.now(clock)) + ".db";
            LOG.atInfo().addKeyValue("backup.bytes", Files.size(file)).log("Database backup downloaded");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build()
                            .toString())
                    .body(Files.readAllBytes(file));
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }
}
