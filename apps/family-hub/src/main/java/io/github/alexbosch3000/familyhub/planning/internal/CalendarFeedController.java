// Public iCalendar feed, authenticated by the member's calendar token embedded in the URL.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.PlanningService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CalendarFeedController {

    private static final MediaType CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final PlanningService planning;

    CalendarFeedController(PlanningService planning) {
        this.planning = planning;
    }

    /** An unknown token answers 404, like a missing feed, so tokens cannot be probed. */
    @GetMapping("/api/planning/feed/{token}.ics")
    ResponseEntity<String> feed(@PathVariable String token) {
        return planning.calendarFeed(token)
                .map(ics -> ResponseEntity.ok().contentType(CALENDAR).cacheControl(CacheControl.noStore()).body(ics))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
