// Session endpoints: bootstrap state for the UI, first-run setup and login.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.family.MemberService.MemberData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {

    private final MemberService members;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    SessionController(MemberService members, AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository) {
        this.members = members;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    record SessionState(boolean setupRequired, Member member) {
    }

    record SetupRequest(
            @NotBlank @Size(max = 60) String name,
            @NotBlank @Pattern(regexp = MemberController.COLOR) String color,
            @NotBlank @Pattern(regexp = MemberController.USERNAME) String username,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** First call of the UI: tells whether setup is needed and who is logged in, and issues the CSRF cookie. */
    @GetMapping("/api/session")
    SessionState session(CsrfToken csrfToken) {
        // Reading the token makes Spring Security write the XSRF-TOKEN cookie the SPA sends back.
        csrfToken.getToken();
        return new SessionState(members.setupRequired(), members.currentMemberId().map(members::get).orElse(null));
    }

    @PostMapping("/api/setup")
    @ResponseStatus(HttpStatus.CREATED)
    Member setup(@Valid @RequestBody SetupRequest request, HttpServletRequest http, HttpServletResponse response) {
        members.setup(new MemberData(request.name(), request.color(), null, request.username(), request.password()));
        return login(new LoginRequest(request.username(), request.password()), http, response);
    }

    @PostMapping("/api/auth/login")
    Member login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        // A new session id on login defeats session fixation.
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, http, response);
        return members.get(((MemberPrincipal) authentication.getPrincipal()).memberId());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ProblemDetail badCredentials(AuthenticationException e) {
        String detail = e instanceof BadCredentialsException
                ? "Identifiant ou mot de passe incorrect." : "Connexion impossible.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }
}
