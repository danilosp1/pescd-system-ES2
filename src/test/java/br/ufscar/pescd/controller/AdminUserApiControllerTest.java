package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.PublicUserResponse;
import br.ufscar.pescd.dto.UserForm;
import br.ufscar.pescd.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminUserApiControllerTest {

    private UserService userService;
    private AdminUserApiController adminUserApiController;

    @BeforeEach
    public void setUp() {
        userService = mock(UserService.class);
        adminUserApiController = new AdminUserApiController(userService);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private PublicUserResponse sampleResponse(Long id) {
        return new PublicUserResponse(id, "Prof", "prof@ufscar.br", "prof", "PROFESSOR", true);
    }

    @Test
    public void listAndFindByIdReturnServiceResults() {
        PublicUserResponse user = sampleResponse(1L);
        when(userService.findAllPublic()).thenReturn(List.of(user));
        when(userService.findPublicById(1L)).thenReturn(user);

        List<PublicUserResponse> list = adminUserApiController.list();
        PublicUserResponse found = adminUserApiController.findById(1L);

        assertEquals(1, list.size());
        assertEquals(user, found);
    }

    @Test
    public void createReturnsCreatedWithLocationHeader() {
        UserForm form = new UserForm();
        PublicUserResponse created = sampleResponse(42L);
        when(userService.createFromApi(form)).thenReturn(created);

        ResponseEntity<PublicUserResponse> response = adminUserApiController.create(form);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(created, response.getBody());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/admin/users/42"));
    }

    @Test
    public void updateSetsIdAndCallsService() {
        UserForm form = new UserForm();
        PublicUserResponse updated = sampleResponse(15L);
        when(userService.updateFromApi(form)).thenReturn(updated);

        PublicUserResponse result = adminUserApiController.update(15L, form);

        assertEquals(15L, form.getId());
        assertEquals(updated, result);
    }

    @Test
    public void deleteCallsServiceWithAuthenticatedUsernameAndReturnsNoContent() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");

        ResponseEntity<Void> response = adminUserApiController.delete(3L, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).delete(3L, "admin");
    }
}
