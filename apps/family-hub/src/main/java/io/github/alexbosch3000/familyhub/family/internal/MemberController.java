// Members API: everyone can list members; parents manage them; each member manages its calendar feed.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.family.Member;
import io.github.alexbosch3000.familyhub.family.MemberService;
import io.github.alexbosch3000.familyhub.family.MemberService.MemberData;
import io.github.alexbosch3000.familyhub.family.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
class MemberController {

    static final String COLOR = "^#[0-9a-fA-F]{6}$";
    static final String USERNAME = "^[a-zA-Z0-9._-]{3,32}$";

    private final MemberService members;

    MemberController(MemberService members) {
        this.members = members;
    }

    record MemberRequest(
            @NotBlank @Size(max = 60) String name,
            @NotBlank @Pattern(regexp = COLOR) String color,
            @NotNull Role role,
            @Nullable @Pattern(regexp = USERNAME) String username,
            @Nullable @Size(min = 8, max = 128) String password,
            @Nullable @Size(max = 20) String clothingSize,
            @Nullable @Size(max = 20) String shoeSize) {

        MemberData toData() {
            return new MemberData(name, color, role, username, password, clothingSize, shoeSize);
        }
    }

    record CalendarToken(String token) {
    }

    @GetMapping
    List<Member> list() {
        return members.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Member create(@Valid @RequestBody MemberRequest request) {
        return members.create(request.toData());
    }

    @PutMapping("/{id}")
    Member update(@PathVariable long id, @Valid @RequestBody MemberRequest request) {
        return members.update(id, request.toData());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        members.delete(id);
    }

    /** Returns the token once; the server only keeps its hash. */
    @PostMapping("/me/calendar-token")
    CalendarToken newCalendarToken() {
        return new CalendarToken(members.regenerateCalendarToken(members.current().id()));
    }

    @DeleteMapping("/me/calendar-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeCalendarToken() {
        members.revokeCalendarToken(members.current().id());
    }
}
