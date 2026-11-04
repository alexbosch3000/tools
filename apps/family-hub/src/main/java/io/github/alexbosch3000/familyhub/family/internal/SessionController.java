// Session endpoints: bootstrap state for the UI, first-run setup, login and logout.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.family.MemberService.MemberData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {

    private static final Logger LOG = LoggerFactory.getLogger(SessionController.class);

    private final MemberService members;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;
    private final LoginThrottle throttle;
    private final AuthMetrics metrics;

    SessionController(MemberService members, AuthenticationManager authenticationManager, TokenService tokens,
            LoginThrottle throttle, AuthMetrics metrics) {
        this.members = members;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
        this.throttle = throttle;
        this.metrics = metrics;
    }

    /** @param member the member behind the token sent, null when none or invalid */
    record SessionState(boolean setupRequired, @Nullable Member member) {
    }

    /** @param token to send as "Authorization: Bearer token"; valid 30 days after its last use */
    record Session(String token, Member member) {
    }

    record SetupRequest(
            @NotBlank @Size(max = 60) String name,
            @NotBlank @Pattern(regexp = MemberController.COLOR) String color,
            @NotBlank @Pattern(regexp = MemberController.USERNAME) String username,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    record LoginRequest(@NotBlank @Size(max = 32) String username, @NotBlank @Size(max = 128) String password) {
    }

    /** First call of the UI: whether setup is needed, and who the token sent belongs to. */
    @GetMapping("/api/session")
    SessionState session() {
        return new SessionState(members.setupRequired(), members.currentMemberId().map(members::get).orElse(null));
    }

    @PostMapping("/api/setup")
    @ResponseStatus(HttpStatus.CREATED)
    Session setup(@Valid @RequestBody SetupRequest request) {
        Member member = members.setup(new MemberData(request.name(), request.color(), null, request.username(),
                request.password(), null, null));
        return new Session(tokens.issue(member.id()), member);
    }

    @PostMapping("/api/auth/login")
    Session login(@Valid @RequestBody LoginRequest request) {
        Duration wait = throttle.retryAfter(request.username());
        if (!wait.isZero() && !wait.isNegative()) {
            metrics.login(AuthMetrics.Outcome.THROTTLED);
            LOG.warn("Login refused: too many failed attempts");
            throw new TooManyAttemptsException(wait);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throttle.failed(request.username());
            metrics.login(AuthMetrics.Outcome.FAILURE);
            LOG.warn("Login failed");
            throw e;
        }
        throttle.succeeded(request.username());
        long memberId = ((MemberPrincipal) authentication.getPrincipal()).memberId();
        metrics.login(AuthMetrics.Outcome.SUCCESS);
        LOG.atInfo().addKeyValue("member.id", memberId).log("Login succeeded");
        return new Session(tokens.issue(memberId), members.get(memberId));
    }

    /** Revokes the token sent; the other devices of the member stay signed in. */
    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            tokens.revoke(authorization.replaceFirst("(?i)^Bearer\\s+", "").trim());
        }
    }

    /** Raised when a username failed too many times recently. */
    static final class TooManyAttemptsException extends RuntimeException {

        private final Duration wait;

        TooManyAttemptsException(Duration wait) {
            super("Too many login attempts");
            this.wait = wait;
        }
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<ProblemDetail> tooManyAttempts(TooManyAttemptsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Trop de tentatives. Réessayez dans " + Math.max(1, e.wait.toMinutes()) + " minutes.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.wait.toSeconds())).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ProblemDetail badCredentials(AuthenticationException e) {
        String detail = e instanceof BadCredentialsException
                ? "Identifiant ou mot de passe incorrect." : "Connexion impossible.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }
}
