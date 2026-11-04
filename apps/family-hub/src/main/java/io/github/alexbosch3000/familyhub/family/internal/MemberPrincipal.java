// Authenticated identity of a request: the member id and role, never the password once checked.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Role;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class MemberPrincipal implements UserDetails, CredentialsContainer {

    private final long memberId;
    private final String username;
    private final Role role;
    private @Nullable String passwordHash;

    MemberPrincipal(long memberId, String username, @Nullable String passwordHash, Role role) {
        this.memberId = memberId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public long memberId() {
        return memberId;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
