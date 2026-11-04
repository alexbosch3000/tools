// Issues, checks and revokes API access tokens. Tokens expire after 30 days without use.
package io.github.alexbosch3000.familyhub.family.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TokenService {

    static final Duration VALIDITY = Duration.ofDays(30);
    /** Extending the expiry at most once a day keeps reads from turning into writes. */
    private static final Duration EXTENSION_STEP = Duration.ofDays(1);

    private final TokenRepository tokens;
    private final Clock clock;

    TokenService(TokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    String issue(long memberId) {
        String token = Secrets.newToken("fh_");
        LocalDateTime now = now();
        tokens.insert(memberId, Secrets.sha256(token), now, now.plus(VALIDITY));
        return token;
    }

    /** The member owning a valid token, whose expiry slides forward with use. */
    @Transactional
    Optional<MemberPrincipal> authenticate(String token) {
        LocalDateTime now = now();
        return tokens.findValid(Secrets.sha256(token), now).map(owner -> {
            LocalDateTime renewed = now.plus(VALIDITY);
            if (owner.expiresAt().plus(EXTENSION_STEP).isBefore(renewed)) {
                tokens.extend(owner.tokenId(), renewed);
            }
            return owner.principal();
        });
    }

    @Transactional
    void revoke(String token) {
        tokens.delete(Secrets.sha256(token));
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "${app.time-zone}")
    @Transactional
    void purgeExpired() {
        tokens.deleteExpired(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }
}
