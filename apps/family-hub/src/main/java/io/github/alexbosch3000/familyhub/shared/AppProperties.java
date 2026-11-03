// Application settings bound from the `app.*` properties, themselves fed by APP_* environment variables.
package io.github.alexbosch3000.familyhub.shared;

import java.nio.file.Path;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param dataDir  directory holding the SQLite database
 * @param timeZone zone in which every local date-time of the family is interpreted
 */
@ConfigurationProperties("app")
public record AppProperties(Path dataDir, ZoneId timeZone) {
}
