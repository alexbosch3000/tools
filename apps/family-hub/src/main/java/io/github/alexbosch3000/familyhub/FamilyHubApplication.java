// Entry point of the family organizer service: one process serving the API and the user interface.
package io.github.alexbosch3000.familyhub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Each direct sub-package is an application module (see docs/architecture.md). Modules only use
 * the public types at the root of each other's package; {@code internal} sub-packages are private.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class FamilyHubApplication {

    protected FamilyHubApplication() {
        // Spring instantiates the configuration class; nothing else should.
    }

    public static void main(String[] args) throws IOException {
        // SQLite creates the database file but not its directory.
        Files.createDirectories(Path.of(System.getenv().getOrDefault("APP_DATA_DIR", "./data")));
        SpringApplication.run(FamilyHubApplication.class, args);
    }
}
