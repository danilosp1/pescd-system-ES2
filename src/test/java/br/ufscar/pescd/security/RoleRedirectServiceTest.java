package br.ufscar.pescd.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoleRedirectServiceTest {

    private RoleRedirectService roleRedirectService;

    @BeforeEach
    public void setUp() {
        roleRedirectService = new RoleRedirectService();
    }

    @Test
    public void resolveRedirectPathReturnsExpectedPathForEachRole() {
        assertEquals("/admin/users", roleRedirectService.resolveRedirectPath(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertEquals("/secretary/offers", roleRedirectService.resolveRedirectPath(
                List.of(new SimpleGrantedAuthority("ROLE_SECRETARIO"))));
        assertEquals("/dashboard/professor", roleRedirectService.resolveRedirectPath(
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))));
        assertEquals("/aluno/ofertas", roleRedirectService.resolveRedirectPath(
                List.of(new SimpleGrantedAuthority("ROLE_ALUNO"))));
        assertEquals("/dashboard", roleRedirectService.resolveRedirectPath(
                Collections.emptyList()));
    }
}
