package br.ufscar.pescd.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class RoleBasedAuthenticationSuccessHandlerTest {

    @Test
    public void onAuthenticationSuccessRedirectsToResolvedRolePath() throws Exception {
        RoleRedirectService roleRedirectService = mock(RoleRedirectService.class);
        RoleBasedAuthenticationSuccessHandler handler = new RoleBasedAuthenticationSuccessHandler(roleRedirectService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);

        when(request.getContextPath()).thenReturn("/pescd");
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(roleRedirectService.resolveRedirectPath(anyCollection())).thenReturn("/admin/users");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/pescd/admin/users");
    }
}
