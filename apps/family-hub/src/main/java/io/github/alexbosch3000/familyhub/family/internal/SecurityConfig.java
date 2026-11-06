// HTTP security: stateless bearer-token authentication, role-based access per API area, CORS and headers.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.shared.AppProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The UI sends its token in the Authorization header, never in a cookie. Browsers attach nothing on
 * their own, so cross-site request forgery has nothing to forge and CSRF tokens are not needed; and
 * the UI can be served from another origin (GitHub Pages), where Safari would drop a session cookie.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final String CSP = "default-src 'self'; img-src 'self' data:; object-src 'none'; "
            + "base-uri 'self'; frame-ancestors 'none'; form-action 'self'";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/session", "/api/setup", "/api/setup/restore", "/api/auth/login").permitAll()
                // Calendar applications cannot log in: the feed is protected by a token in its URL.
                .requestMatchers(HttpMethod.GET, "/api/planning/feed/*").permitAll()
                .requestMatchers("/api/accounting/**", "/api/admin/**").hasRole("PARENT")
                .requestMatchers(HttpMethod.GET, "/api/members").authenticated()
                .requestMatchers("/api/members/me/**").authenticated()
                .requestMatchers("/api/members/**").hasRole("PARENT")
                .requestMatchers("/api/**").authenticated()
                // UI files, /healthz, /readyz, the exposed actuator endpoints and the API description.
                .anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> { })
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .addFilterBefore(new TokenAuthenticationFilter(tokens), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()")));
        return http.build();
    }

    /** Only the configured UI origins may call the API from a browser; no credentials are involved. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(properties.allowedOrigins());
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        api.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        api.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        api.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", api);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(MemberRepository members) {
        return username -> members.findPrincipal(username)
                .filter(principal -> principal.getPassword() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown member"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
