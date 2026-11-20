package br.ufscar.pescd.controller;

import br.ufscar.pescd.security.RoleRedirectService;
import br.ufscar.pescd.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private OfferService offerService;
    private RoleRedirectService roleRedirectService;
    private AuthController authController;

    @BeforeEach
    public void setUp() {
        offerService = mock(OfferService.class);
        roleRedirectService = mock(RoleRedirectService.class);
        authController = new AuthController(offerService, roleRedirectService);
    }

    @Test
    public void simpleViewEndpointsReturnExpectedTemplates() {
        assertEquals("login", authController.login());
        assertEquals("access-denied", authController.accessDenied());
        assertEquals("dashboard", authController.secretarioDashboard());
        assertEquals("dashboard", authController.professorDashboard());
    }

    @Test
    public void homeAddsPublicOffersAndReturnsIndex() {
        Model model = mock(Model.class);
        when(offerService.findPublicOffers()).thenReturn(Collections.emptyList());

        String view = authController.home(model);

        assertEquals("index", view);
        verify(model).addAttribute("offers", Collections.emptyList());
    }

    @Test
    public void dashboardRedirectsToLoginWhenUnauthenticated() {
        assertEquals("redirect:/login", authController.dashboard(null));

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        assertEquals("redirect:/login", authController.dashboard(auth));
    }

    @Test
    public void dashboardRedirectsToRolePathWhenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        when(roleRedirectService.resolveRedirectPath(anyCollection())).thenReturn("/admin/users");

        assertEquals("redirect:/admin/users", authController.dashboard(auth));
    }
}
