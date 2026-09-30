package ch.kmu.debitoren.shared.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * OAuth2 Resource Server mit JWT von Keycloak. Rollen prüfen die Use Cases per {@code @PreAuthorize};
 * hier wird nur verlangt, dass jede Anfrage angemeldet ist.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityKonfiguration {

    /**
     * Actuator läuft auf eigenem Port (management.server.port), den der Ingress nicht nach aussen gibt.
     * Health dient Kubernetes als Probe, Prometheus liest die Metriken; alles andere bleibt gesperrt.
     */
    @Bean
    @Order(1)
    SecurityFilterChain actuatorKette(HttpSecurity http) {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(regeln -> regeln.requestMatchers(EndpointRequest.to("health", "prometheus"))
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .sessionManagement(sitzung -> sitzung.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiKette(
            HttpSecurity http,
            ProblemAntwort problemAntwort,
            @Value("${debitoren.api-docs-oeffentlich:false}") boolean apiDocsOeffentlich) {
        return http.authorizeHttpRequests(regeln -> {
                    if (apiDocsOeffentlich) {
                        regeln.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll();
                    }
                    regeln.anyRequest().authenticated();
                })
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtKonverter()))
                        .authenticationEntryPoint(problemAntwort)
                        .accessDeniedHandler(problemAntwort))
                .exceptionHandling(fehler ->
                        fehler.authenticationEntryPoint(problemAntwort).accessDeniedHandler(problemAntwort))
                .sessionManagement(sitzung -> sitzung.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Kein Cookie, nur Bearer-Token: CSRF kann hier nicht auftreten.
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    ProblemAntwort problemAntwort(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver fehlerbehandlung) {
        return new ProblemAntwort(fehlerbehandlung);
    }

    @Bean
    static RoleHierarchy rollenHierarchie() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SYSTEM")
                .implies("ADMIN")
                .role("ADMIN")
                .implies("BUCHHALTUNG")
                .role("BUCHHALTUNG")
                .implies("LESER")
                .build();
    }

    static JwtAuthenticationConverter jwtKonverter() {
        var konverter = new JwtAuthenticationConverter();
        konverter.setJwtGrantedAuthoritiesConverter(new KeycloakRollenConverter());
        konverter.setPrincipalClaimName("preferred_username");
        return konverter;
    }
}
