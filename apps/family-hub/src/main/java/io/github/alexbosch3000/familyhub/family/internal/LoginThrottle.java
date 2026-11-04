// Slows down password guessing: five failed logins for a username lock it for fifteen minutes.
package io.github.alexbosch3000.familyhub.family.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Kept in memory: a restart clears it, which is acceptable for a single family service. */
@Component
class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    /** Time to wait before the next attempt, zero when an attempt is allowed. */
    Duration retryAfter(String username) {
        Deque<Instant> recent = failures.get(key(username));
        if (recent == null) {
            return Duration.ZERO;
        }
        synchronized (recent) {
            prune(recent);
            if (recent.size() < MAX_FAILURES) {
                return Duration.ZERO;
            }
            return Duration.between(clock.instant(), recent.peekFirst().plus(WINDOW));
        }
    }

    void failed(String username) {
        Deque<Instant> recent = failures.computeIfAbsent(key(username), k -> new ArrayDeque<>());
        synchronized (recent) {
            prune(recent);
            recent.addLast(clock.instant());
        }
    }

    void succeeded(String username) {
        failures.remove(key(username));
    }

    private void prune(Deque<Instant> recent) {
        Instant limit = clock.instant().minus(WINDOW);
        while (!recent.isEmpty() && !recent.peekFirst().isAfter(limit)) {
            recent.removeFirst();
        }
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
