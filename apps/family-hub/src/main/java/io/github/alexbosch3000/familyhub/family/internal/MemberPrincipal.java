// Authenticated identity kept in the session: the member id and role, without the password once logged in.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class MemberPrincipal implements UserDetails, CredentialsContainer {

    private final long memberId;
    private final String username;
    private final Role role;
    private String passwordHash;

    MemberPrincipal(long memberId, String username, String passwordHash, Role role) {
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
    public String getPassword() {
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
