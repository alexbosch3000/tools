// Public API of the family module: members, the logged-in member, first-run setup and calendar tokens.
package io.github.alexbosch3000.familyhub.family;

import io.github.alexbosch3000.familyhub.family.internal.MemberPrincipal;
import io.github.alexbosch3000.familyhub.family.internal.MemberRepository;
import io.github.alexbosch3000.familyhub.family.internal.Secrets;
import io.github.alexbosch3000.familyhub.family.internal.TokenRepository;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository repository;
    private final TokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    MemberService(MemberRepository repository, TokenRepository tokens, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Values of a member to create or update. A null password keeps the current one on update. */
    public record MemberData(String name, String color, Role role, @Nullable String username,
            @Nullable String password, @Nullable String clothingSize, @Nullable String shoeSize) {
    }

    public List<Member> list() {
        return repository.findAll();
    }

    public Member get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Membre introuvable."));
    }

    public boolean exists(long id) {
        return repository.findById(id).isPresent();
    }

    /** Identifier of the member behind the current request, empty outside an authenticated request. */
    public Optional<Long> currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof MemberPrincipal principal) {
            return Optional.of(principal.memberId());
        }
        return Optional.empty();
    }

    public Member current() {
        return currentMemberId().map(this::get)
                .orElseThrow(() -> new NotFoundException("Aucun membre connecté."));
    }

    public boolean setupRequired() {
        return repository.count() == 0;
    }

    /** Creates the first parent; refused once the family has a member. */
    @Transactional
    public Member setup(MemberData data) {
        if (!setupRequired()) {
            throw new ConflictException("La famille est déjà configurée.");
        }
        if (data.username() == null || data.password() == null) {
            throw new InvalidRequestException("Le premier parent doit avoir un identifiant et un mot de passe.");
        }
        return create(new MemberData(data.name(), data.color(), Role.PARENT, data.username(), data.password(),
                data.clothingSize(), data.shoeSize()));
    }

    @Transactional
    public Member create(MemberData data) {
        String username = normalize(data.username());
        validateLogin(username, data.password(), true);
        ensureUsernameFree(username, null);
        long id = repository.insert(normalized(data, username), hash(data.password()), LocalDateTime.now(clock));
        return get(id);
    }

    /** Changing a password or removing the login signs the member out of every device. */
    @Transactional
    public Member update(long id, MemberData data) {
        Member existing = get(id);
        String username = normalize(data.username());
        if (currentMemberId().filter(current -> current == id).isPresent() && data.role() != existing.role()) {
            throw new InvalidRequestException("Vous ne pouvez pas changer votre propre rôle.");
        }
        boolean newLogin = existing.username() == null && username != null;
        validateLogin(username, data.password(), newLogin);
        ensureUsernameFree(username, id);
        repository.update(id, normalized(data, username));
        if (username == null) {
            repository.updatePassword(id, null);
            tokens.deleteForMember(id);
        } else if (data.password() != null) {
            repository.updatePassword(id, hash(data.password()));
            tokens.deleteForMember(id);
        }
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        get(id);
        if (currentMemberId().filter(current -> current == id).isPresent()) {
            throw new InvalidRequestException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        repository.delete(id);
    }

    /** Issues a new calendar subscription token for a member; the previous one stops working. */
    @Transactional
    public String regenerateCalendarToken(long memberId) {
        get(memberId);
        String token = Secrets.newToken("fhc_");
        repository.updateCalendarTokenHash(memberId, Secrets.sha256(token));
        return token;
    }

    @Transactional
    public void revokeCalendarToken(long memberId) {
        repository.updateCalendarTokenHash(memberId, null);
    }

    public Optional<Member> findByCalendarToken(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repository.findByCalendarTokenHash(Secrets.sha256(token));
    }

    private static MemberData normalized(MemberData data, @Nullable String username) {
        return new MemberData(data.name().trim(), data.color(), data.role(), username, null,
                blankToNull(data.clothingSize()), blankToNull(data.shoeSize()));
    }

    private void validateLogin(@Nullable String username, @Nullable String password, boolean passwordRequired) {
        if (username != null && passwordRequired && password == null) {
            throw new InvalidRequestException("Un mot de passe est requis pour un membre qui se connecte.");
        }
        if (username == null && password != null) {
            throw new InvalidRequestException("Un mot de passe sans identifiant n'a pas de sens.");
        }
    }

    private void ensureUsernameFree(@Nullable String username, @Nullable Long exceptId) {
        if (username != null && repository.usernameTaken(username, exceptId)) {
            throw new ConflictException("Cet identifiant est déjà utilisé.");
        }
    }

    private @Nullable String hash(@Nullable String password) {
        return password == null ? null : passwordEncoder.encode(password);
    }

    private static @Nullable String normalize(@Nullable String username) {
        return username == null || username.isBlank() ? null : username.trim().toLowerCase();
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
