// Replaces the live database with an uploaded backup after checking that it is a usable family-hub backup.
package io.github.alexbosch3000.familyhub.admin.internal;

import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.shared.AppProperties;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Not transactional: the database is replaced page by page through SQLite's online backup API, then
 * brought to the current schema by Flyway. Either step failing leaves the service on whatever the
 * database then holds, which is why the upload is fully validated before the live database is touched.
 */
@Service
class RestoreService {

    private static final Logger LOG = LoggerFactory.getLogger(RestoreService.class);
    private static final long MAX_BYTES = 200L * 1024 * 1024;
    private static final String NOT_A_BACKUP = "Ce fichier n'est pas une sauvegarde family-hub valide.";

    private final AppProperties properties;
    private final JdbcTemplate jdbc;
    private final Flyway flyway;
    private final MemberService members;

    RestoreService(AppProperties properties, JdbcTemplate jdbc, Flyway flyway, MemberService members) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.flyway = flyway;
        this.members = members;
    }

    /** Whether the family has no member yet, which is when a backup may be restored without signing in. */
    boolean freshInstance() {
        return members.setupRequired();
    }

    @WithSpan
    void restore(InputStream body) throws IOException {
        Path file = properties.dataDir().toAbsolutePath().resolve("restore-" + UUID.randomUUID() + ".db");
        try {
            long bytes = store(body, file);
            validate(file);
            replaceLiveDatabase(file);
            flyway.migrate();
            LOG.atInfo().addKeyValue("backup.bytes", bytes).log("Database restored from a backup");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(Path.of(file + "-wal"));
            Files.deleteIfExists(Path.of(file + "-shm"));
            Files.deleteIfExists(Path.of(file + "-journal"));
        }
    }

    private static long store(InputStream body, Path file) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream out = Files.newOutputStream(file)) {
            for (int read = body.read(buffer); read >= 0; read = body.read(buffer)) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new InvalidRequestException("Fichier trop volumineux.");
                }
                out.write(buffer, 0, read);
            }
        }
        return total;
    }

    /** Opens the upload on its own connection: integrity, schema version not newer than ours, one parent. */
    private void validate(Path file) {
        MigrationVersion backupVersion;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("PRAGMA integrity_check")) {
                if (!rs.next() || !"ok".equals(rs.getString(1))) {
                    throw new InvalidRequestException(NOT_A_BACKUP);
                }
            }
            backupVersion = latestVersion(statement);
            try (ResultSet rs = statement.executeQuery("""
                    SELECT COUNT(*) FROM member
                    WHERE role = 'PARENT' AND username IS NOT NULL AND password_hash IS NOT NULL""")) {
                if (!rs.next() || rs.getInt(1) < 1) {
                    throw new InvalidRequestException(NOT_A_BACKUP);
                }
            }
        } catch (SQLException e) {
            throw new InvalidRequestException(NOT_A_BACKUP);
        }
        MigrationInfo current = flyway.info().current();
        if (current != null && backupVersion.compareTo(current.getVersion()) > 0) {
            throw new ConflictException(
                    "Sauvegarde d'une version plus récente : mettez family-hub à jour avant de restaurer.");
        }
    }

    private static MigrationVersion latestVersion(Statement statement) throws SQLException {
        MigrationVersion latest = null;
        try (ResultSet rs = statement.executeQuery(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL")) {
            while (rs.next()) {
                MigrationVersion version = MigrationVersion.fromVersion(rs.getString(1));
                if (latest == null || version.compareTo(latest) > 0) {
                    latest = version;
                }
            }
        }
        if (latest == null) {
            throw new InvalidRequestException(NOT_A_BACKUP);
        }
        return latest;
    }

    /** The sqlite-jdbc statement {@code restore from} copies the file into the connection's database. */
    private void replaceLiveDatabase(Path file) {
        if (file.toString().contains("'")) {
            throw new InvalidRequestException("Chemin de données non pris en charge.");
        }
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("restore from '" + file + "'");
            }
            return null;
        });
    }
}
