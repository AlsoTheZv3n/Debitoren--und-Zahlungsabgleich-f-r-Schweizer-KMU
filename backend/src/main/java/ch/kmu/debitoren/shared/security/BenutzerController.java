package ch.kmu.debitoren.shared.security;

import ch.kmu.debitoren.shared.mandant.MandantClaim;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Benutzer")
class BenutzerController {

    private final RoleHierarchy rollenHierarchie;

    BenutzerController(RoleHierarchy rollenHierarchie) {
        this.rollenHierarchie = rollenHierarchie;
    }

    /** Liefert die wirksamen Rollen, also inklusive der Rollen, die eine höhere Rolle einschliesst. */
    @GetMapping
    @Operation(
            operationId = "aktuellerBenutzer",
            summary = "Angemeldeter Benutzer mit wirksamen Rollen und erlaubten Mandanten")
    BenutzerDto aktuellerBenutzer(JwtAuthenticationToken anmeldung) {
        var jwt = anmeldung.getToken();
        var benutzername =
                Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(anmeldung.getName());
        var rollen = rollenHierarchie.getReachableGrantedAuthorities(anmeldung.getAuthorities()).stream()
                .map(GrantedAuthority::getAuthority)
                .map(Rolle::ausAuthority)
                .flatMap(Optional::stream)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new BenutzerDto(
                benutzername,
                Optional.ofNullable(jwt.getClaimAsString("name")).orElse(benutzername),
                jwt.getClaimAsString("email"),
                rollen,
                MandantClaim.lesen(jwt));
    }
}
