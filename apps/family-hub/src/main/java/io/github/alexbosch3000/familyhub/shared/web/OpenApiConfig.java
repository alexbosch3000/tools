// OpenAPI description of the API: the contract from which the TypeScript client of the UI is generated.
package io.github.alexbosch3000.familyhub.shared.web;

import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    static {
        // Named enum schemas become named TypeScript types instead of repeated string unions.
        ModelResolver.enumsAsRef = true;
    }

    @Bean
    OpenAPI familyHubOpenApi() {
        return new OpenAPI()
                .info(new Info().title("family-hub").version("1")
                        .description("Family organizer API. Send the token returned by /api/auth/login"
                                + " as a Bearer token."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                // Relative: the same contract describes every deployment.
                .servers(List.of(new Server().url("/")));
    }
}
