package io.github.alexbosch3000.dupscan.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * SQLite-backed {@link IndexStore} in WAL mode. A single dedicated write connection
 * (guarded by {@link #writeLock}) serialises all mutations, while reads use per-thread
 * read connections so the pipeline's worker threads can check currency concurrently. WAL
 * permits concurrent readers alongside the single writer without corruption.
 */
public final class SqliteIndexStore implements IndexStore {

    private static final Logger log = LoggerFactory.getLogger(SqliteIndexStore.class);

    private static final String UPSERT = """
            INSERT INTO images(canonical_path, size_bytes, modified_millis, content_sha256,
                               perceptual_hash, width, height, format, exif_millis, last_seen)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(canonical_path) DO UPDATE SET
                size_bytes=excluded.size_bytes,
                modified_millis=excluded.modified_millis,
                content_sha256=excluded.content_sha256,
                perceptual_hash=excluded.perceptual_hash,
                width=excluded.width,
                height=excluded.height,
                format=excluded.format,
                exif_millis=excluded.exif_millis,
                last_seen=excluded.last_seen
            """;

    private final String url;
    private final Object writeLock = new Object();
    private final Connection writeConnection;
    private final ThreadLocal<Connection> readConnection;
    private final List<Connection> allConnections = new CopyOnWriteArrayList<>();

    public SqliteIndexStore(Path dbFile) {
        this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        try {
            this.writeConnection = open();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot open index at " + dbFile, e);
        }
        this.readConnection = ThreadLocal.withInitial(() -> {
            try {
                return open();
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot open read connection", e);
            }
        });
    }

    private Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
            s.execute("PRAGMA busy_timeout=30000");
        }
        allConnections.add(c);
        return c;
    }

    @Override
    public void initialize() {
        synchronized (writeLock) {
            try (Statement s = writeConnection.createStatement()) {
                s.execute("""
                        CREATE TABLE IF NOT EXISTS images(
                            canonical_path  TEXT PRIMARY KEY,
                            size_bytes      INTEGER NOT NULL,
                            modified_millis INTEGER NOT NULL,
                            content_sha256  TEXT,
                            perceptual_hash INTEGER,
                            width           INTEGER NOT NULL DEFAULT 0,
                            height          INTEGER NOT NULL DEFAULT 0,
                            format          TEXT,
                            exif_millis     INTEGER,
                            last_seen       INTEGER NOT NULL DEFAULT 0
                        )""");
                s.execute("CREATE INDEX IF NOT EXISTS idx_content ON images(content_sha256)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_phash ON images(perceptual_hash)");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to initialize index schema", e);
            }
        }
    }

    @Override
    public Optional<ImageRecord> lookup(String canonicalPath) {
        String sql = "SELECT * FROM images WHERE canonical_path=?";
        try (PreparedStatement ps = readConnection.get().prepareStatement(sql)) {
            ps.setString(1, canonicalPath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("lookup failed for " + canonicalPath, e);
        }
    }

    @Override
    public void upsertBatch(List<ImageRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        synchronized (writeLock) {
            try {
                writeConnection.setAutoCommit(false);
                try (PreparedStatement ps = writeConnection.prepareStatement(UPSERT)) {
                    for (ImageRecord r : records) {
                        bind(ps, r);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                writeConnection.commit();
            } catch (SQLException e) {
                rollbackQuietly();
                throw new IllegalStateException("upsertBatch failed", e);
            } finally {
                resetAutoCommit();
            }
        }
    }

    @Override
    public void markSeen(List<String> canonicalPaths, long runId) {
        if (canonicalPaths.isEmpty()) {
            return;
        }
        synchronized (writeLock) {
            try {
                writeConnection.setAutoCommit(false);
                try (PreparedStatement ps = writeConnection.prepareStatement(
                        "UPDATE images SET last_seen=? WHERE canonical_path=?")) {
                    for (String p : canonicalPaths) {
                        ps.setLong(1, runId);
                        ps.setString(2, p);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                writeConnection.commit();
            } catch (SQLException e) {
                rollbackQuietly();
                throw new IllegalStateException("markSeen failed", e);
            } finally {
                resetAutoCommit();
            }
        }
    }

    @Override
    public int pruneNotSeen(long runId, List<String> rootPrefixes) {
        if (rootPrefixes.isEmpty()) {
            return 0;
        }
        int removed = 0;
        synchronized (writeLock) {
            try (PreparedStatement ps = writeConnection.prepareStatement(
                    "DELETE FROM images WHERE last_seen<>? AND canonical_path LIKE ? ESCAPE '\\'")) {
                for (String prefix : rootPrefixes) {
                    ps.setLong(1, runId);
                    ps.setString(2, escapeLike(prefix) + "%");
                    removed += ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("pruneNotSeen failed", e);
            }
        }
        return removed;
    }

    @Override
    public void forEachHashed(Consumer<ImageRecord> consumer) {
        stream("SELECT * FROM images WHERE content_sha256 IS NOT NULL ORDER BY content_sha256", consumer);
    }

    @Override
    public void forEachWithPerceptualHash(Consumer<ImageRecord> consumer) {
        stream("SELECT * FROM images WHERE perceptual_hash IS NOT NULL", consumer);
    }

    private void stream(String sql, Consumer<ImageRecord> consumer) {
        try (Statement st = readConnection.get().createStatement()) {
            st.setFetchSize(1000);
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    consumer.accept(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("stream query failed", e);
        }
    }

    private static void bind(PreparedStatement ps, ImageRecord r) throws SQLException {
        ps.setString(1, r.canonicalPath());
        ps.setLong(2, r.sizeBytes());
        ps.setLong(3, r.modifiedMillis());
        ps.setString(4, r.contentSha256());
        if (r.perceptualHash() == null) {
            ps.setNull(5, java.sql.Types.INTEGER);
        } else {
            ps.setLong(5, r.perceptualHash());
        }
        ps.setInt(6, r.width());
        ps.setInt(7, r.height());
        ps.setString(8, r.format());
        if (r.exifMillis() == null) {
            ps.setNull(9, java.sql.Types.INTEGER);
        } else {
            ps.setLong(9, r.exifMillis());
        }
        ps.setLong(10, 0L); // last_seen set by markSeen / overwritten on reuse
    }

    private static ImageRecord map(ResultSet rs) throws SQLException {
        long phashRaw = rs.getLong("perceptual_hash");
        Long phash = rs.wasNull() ? null : phashRaw;
        long exifRaw = rs.getLong("exif_millis");
        Long exif = rs.wasNull() ? null : exifRaw;
        return new ImageRecord(
                rs.getString("canonical_path"),
                rs.getLong("size_bytes"),
                rs.getLong("modified_millis"),
                rs.getString("content_sha256"),
                phash,
                rs.getInt("width"),
                rs.getInt("height"),
                rs.getString("format"),
                exif);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void rollbackQuietly() {
        try {
            writeConnection.rollback();
        } catch (SQLException ex) {
            log.warn("rollback failed: {}", ex.toString());
        }
    }

    private void resetAutoCommit() {
        try {
            writeConnection.setAutoCommit(true);
        } catch (SQLException ex) {
            log.warn("could not reset autocommit: {}", ex.toString());
        }
    }

    @Override
    public void close() {
        List<Connection> toClose = new ArrayList<>(allConnections);
        for (Connection c : toClose) {
            try {
                c.close();
            } catch (SQLException e) {
                log.warn("Error closing connection: {}", e.toString());
            }
        }
        allConnections.clear();
    }
}
