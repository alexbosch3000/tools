// SQL access to API access tokens, stored by hash.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Role;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRepository {

    private final JdbcClient jdbc;

    TokenRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** A valid token with the member it belongs to. */
    record TokenOwner(long tokenId, LocalDateTime expiresAt, MemberPrincipal principal) {
    }

    void insert(long memberId, String hash, LocalDateTime now, LocalDateTime expiresAt) {
        jdbc.sql("INSERT INTO auth_token (member_id, token_hash, created_at, expires_at) VALUES (?, ?, ?, ?)")
                .params(memberId, hash, Db.text(now), Db.text(expiresAt)).update();
    }

    Optional<TokenOwner> findValid(String hash, LocalDateTime now) {
        return jdbc.sql("""
                SELECT t.id, t.expires_at, m.id AS member_id, m.username, m.role
                FROM auth_token t JOIN member m ON m.id = t.member_id
                WHERE t.token_hash = ? AND t.expires_at > ? AND m.username IS NOT NULL""")
                .params(hash, Db.text(now))
                .query((rs, n) -> new TokenOwner(rs.getLong("id"), Db.dateTime(rs, "expires_at"),
                        new MemberPrincipal(rs.getLong("member_id"), rs.getString("username"), null,
                                Role.valueOf(rs.getString("role")))))
                .optional();
    }

    void extend(long tokenId, LocalDateTime expiresAt) {
        jdbc.sql("UPDATE auth_token SET expires_at = ? WHERE id = ?").params(Db.text(expiresAt), tokenId).update();
    }

    void delete(String hash) {
        jdbc.sql("DELETE FROM auth_token WHERE token_hash = ?").param(hash).update();
    }

    public void deleteForMember(long memberId) {
        jdbc.sql("DELETE FROM auth_token WHERE member_id = ?").param(memberId).update();
    }

    int deleteExpired(LocalDateTime now) {
        return jdbc.sql("DELETE FROM auth_token WHERE expires_at <= ?").param(Db.text(now)).update();
    }
}
