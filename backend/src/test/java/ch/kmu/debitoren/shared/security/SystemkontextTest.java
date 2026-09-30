package ch.kmu.debitoren.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SystemkontextTest {

    @AfterEach
    void aufraeumen() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meldetDasSystemNurWaehrendDerAktionAn() {
        var authorities = Systemkontext.ausfuehren(
                () -> SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());

        assertThat(authorities).containsExactly(Systemkontext.AUTHORITY);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void systemRolleSchliesstAlleFachrollenEin() {
        var erreichbar = SecurityKonfiguration.rollenHierarchie()
                .getReachableGrantedAuthorities(Systemkontext.ausfuehren(() ->
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities()))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(erreichbar)
                .contains(Rolle.ADMIN.authority(), Rolle.BUCHHALTUNG.authority(), Rolle.LESER.authority());
    }
}
