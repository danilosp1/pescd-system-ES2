package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.AuthLoginRequest;
import br.ufscar.pescd.dto.AuthResponse;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.security.RoleRedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthApiControllerTest {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RoleRedirectService roleRedirectService;
    private AuthApiController authApiController;

    @BeforeEach
    public void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        roleRedirectService = mock(RoleRedirectService.class);
        authApiController = new AuthApiController(authenticationManager, userRepository, roleRedirectService);
    }

    private User createAdminUser() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Admin");
        user.setEmail("admin@pescd.com");
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void loginAuthenticatesAndCreatesSession() {
        AuthLoginRequest request = new AuthLoginRequest("admin", "admin123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(httpRequest.getSession(true)).thenReturn(session);
        when(authentication.getName()).thenReturn("admin");
        List authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(createAdminUser()));
        when(roleRedirectService.resolveRedirectPath(anyCollection())).thenReturn("/admin/users");

        ResponseEntity<AuthResponse> response = authApiController.login(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("admin", response.getBody().username());
        assertEquals("/admin/users", response.getBody().redirectPath());
        verify(session).setAttribute(anyString(), any());
    }

    @Test
    public void loginThrowsWhenUserNotFoundAfterAuthentication() {
        AuthLoginRequest request = new AuthLoginRequest("admin", "admin123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(httpRequest.getSession(true)).thenReturn(session);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            authApiController.login(request, httpRequest);
        });
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void meReturnsCurrentAuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        List authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(createAdminUser()));
        when(roleRedirectService.resolveRedirectPath(anyCollection())).thenReturn("/admin/users");

        ResponseEntity<AuthResponse> response = authApiController.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    public void meThrowsWhenUserNotFound() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("inexistente");
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            authApiController.me(authentication);
        });
    }

    @Test
    public void logoutHandlesAuthenticatedAndUnauthenticatedRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Authentication authentication = mock(Authentication.class);

        when(request.getSession(false)).thenReturn(session);

        ResponseEntity<Void> resAuth = authApiController.logout(request, response, authentication);
        assertEquals(HttpStatus.NO_CONTENT, resAuth.getStatusCode());

        ResponseEntity<Void> resNoAuthWithSession = authApiController.logout(request, response, null);
        assertEquals(HttpStatus.NO_CONTENT, resNoAuthWithSession.getStatusCode());
        verify(session, atLeastOnce()).invalidate();

        when(request.getSession(false)).thenReturn(null);
        ResponseEntity<Void> resNoAuthNoSession = authApiController.logout(request, response, null);
        assertEquals(HttpStatus.NO_CONTENT, resNoAuthNoSession.getStatusCode());
    }
}
