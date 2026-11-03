// SQL access to the member table.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.Role;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    private static final String COLUMNS =
            "id, name, color, role, username, calendar_token_hash IS NOT NULL AS has_feed";

    private final JdbcClient jdbc;

    MemberRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Member> findAll() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM member ORDER BY role DESC, name").query(MemberRepository::map)
                .list();
    }

    public Optional<Member> findById(long id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM member WHERE id = ?").param(id).query(MemberRepository::map)
                .optional();
    }

    public Optional<Member> findByCalendarTokenHash(String hash) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM member WHERE calendar_token_hash = ?").param(hash)
                .query(MemberRepository::map).optional();
    }

    Optional<MemberPrincipal> findPrincipal(String username) {
        return jdbc.sql("SELECT id, username, password_hash, role FROM member WHERE username = ?")
                .param(username.trim().toLowerCase())
                .query((rs, n) -> new MemberPrincipal(rs.getLong("id"), rs.getString("username"),
                        rs.getString("password_hash"), Role.valueOf(rs.getString("role"))))
                .optional();
    }

    public long count() {
        return jdbc.sql("SELECT COUNT(*) FROM member").query(Long.class).single();
    }

    public boolean usernameTaken(String username, Long exceptId) {
        return jdbc.sql("SELECT COUNT(*) FROM member WHERE username = ? AND id IS NOT ?")
                .params(username, exceptId).query(Long.class).single() > 0;
    }

    public long insert(String name, String color, Role role, String username, String passwordHash,
            LocalDateTime now) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO member (name, color, role, username, password_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?)""")
                .params(name, color, role.name(), username, passwordHash, Db.text(now))
                .update(keys);
        return keys.getKey().longValue();
    }

    public void update(long id, String name, String color, Role role, String username) {
        jdbc.sql("UPDATE member SET name = ?, color = ?, role = ?, username = ? WHERE id = ?")
                .params(name, color, role.name(), username, id).update();
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.sql("UPDATE member SET password_hash = ? WHERE id = ?").params(passwordHash, id).update();
    }

    public void updateCalendarTokenHash(long id, String hash) {
        jdbc.sql("UPDATE member SET calendar_token_hash = ? WHERE id = ?").params(hash, id).update();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM member WHERE id = ?").param(id).update();
    }

    private static Member map(ResultSet rs, int row) throws SQLException {
        return new Member(rs.getLong("id"), rs.getString("name"), rs.getString("color"),
                Role.valueOf(rs.getString("role")), rs.getString("username"), rs.getBoolean("has_feed"));
    }
}
