package ch.kmu.debitoren.shared.web;

import ch.kmu.debitoren.shared.mandant.MandantFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Grundangaben der OpenAPI-Beschreibung, aus der das Frontend seinen Client generiert. */
@Configuration(proxyBeanMethods = false)
class OpenApiKonfiguration {

    private static final String SICHERHEITSSCHEMA = "keycloak";
    private static final String PROBLEM = "Problem";

    @Bean
    OpenAPI debitorenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Debitoren API")
                        .version("v1")
                        .description("Debitoren- und Zahlungsabgleich für Schweizer KMU. Anfragen tragen ein"
                                + " Bearer-Token von Keycloak; bei mehreren Mandanten wählt der Header "
                                + MandantFilter.HEADER
                                + " den Mandanten."))
                // Relative Server-URL: SPA und API teilen sich eine Origin.
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(
                                SICHERHEITSSCHEMA,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SICHERHEITSSCHEMA));
    }

    /** Jede Operation kann mit einem ProblemDetail nach RFC 9457 antworten. */
    @Bean
    OpenApiCustomizer problemAlsStandardantwort() {
        var antwort = new ApiResponse()
                .description("Fehler als ProblemDetail (RFC 9457) mit fachlichem Code")
                .content(new Content()
                        .addMediaType(
                                "application/problem+json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM))));
        // Schema erst hier ergänzen: springdoc entfernt vorher alle Schemas, auf die noch nichts verweist.
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            openApi.getPaths().values().stream()
                    .flatMap(pfad -> pfad.readOperations().stream())
                    .forEach(operation -> operation.getResponses().addApiResponse("default", antwort));
        };
    }

    private static Schema<?> problemSchema() {
        var feldfehler = new ObjectSchema();
        feldfehler.addProperty("feld", new StringSchema());
        feldfehler.addProperty("meldung", new StringSchema());
        feldfehler.setRequired(List.of("feld", "meldung"));

        var problem = new ObjectSchema();
        problem.setDescription("Fehlerantwort nach RFC 9457");
        problem.addProperty("type", new StringSchema().format("uri"));
        problem.addProperty("title", new StringSchema());
        problem.addProperty("status", new IntegerSchema());
        problem.addProperty("detail", new StringSchema());
        problem.addProperty("instance", new StringSchema());
        problem.addProperty("code", new StringSchema().description("Fachlicher Code, etwa MANDANT_NICHT_ERLAUBT"));
        problem.addProperty(
                "fehler", new ArraySchema().items(feldfehler).description("Feldfehler bei VALIDIERUNG_FEHLGESCHLAGEN"));
        problem.setRequired(List.of("title", "status", "code"));
        return problem;
    }
}
