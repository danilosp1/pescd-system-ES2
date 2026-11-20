package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.UserForm;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AdminUserControllerTest {

    private UserService userService;
    private AdminUserController adminUserController;

    @BeforeEach
    public void setUp() {
        userService = mock(UserService.class);
        adminUserController = new AdminUserController(userService);
    }

    @Test
    public void rolesReturnsAllowedAdminRoles() {
        assertArrayEquals(
                new UserRole[]{UserRole.ADMIN, UserRole.SECRETARIO, UserRole.PROFESSOR},
                adminUserController.roles()
        );
    }

    @Test
    public void listPopulatesModelAndReturnsListView() {
        Model model = mock(Model.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(userService.findAll()).thenReturn(Collections.emptyList());

        String view = adminUserController.list(model, auth);

        assertEquals("admin/users/list", view);
        verify(model).addAttribute("users", Collections.emptyList());
        verify(model).addAttribute("loggedUsername", "admin");
    }

    @Test
    public void createFormPopulatesEmptyFormAndReturnsFormView() {
        Model model = mock(Model.class);

        String view = adminUserController.createForm(model);

        assertEquals("admin/users/form", view);
        verify(model).addAttribute(eq("userForm"), any(UserForm.class));
        verify(model).addAttribute("isEdit", false);
    }

    @Test
    public void createReturnsFormWhenValidationFailsAndRedirectsWhenValid() {
        UserForm form = new UserForm();
        BindingResult br = mock(BindingResult.class);
        Model model = mock(Model.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(userService.validateBusinessRules(form, br, false)).thenReturn(false);
        assertEquals("admin/users/form", adminUserController.create(form, br, model, redirectAttributes));
        verify(model).addAttribute("isEdit", false);
        verify(userService, never()).create(any());

        when(userService.validateBusinessRules(form, br, false)).thenReturn(true);
        assertEquals("redirect:/admin/users", adminUserController.create(form, br, model, redirectAttributes));
        verify(userService).create(form);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    public void editFormPopulatesExistingUserFormAndReturnsFormView() {
        Model model = mock(Model.class);
        User user = new User();
        UserForm form = new UserForm();
        when(userService.findById(5L)).thenReturn(user);
        when(userService.toForm(user)).thenReturn(form);

        String view = adminUserController.editForm(5L, model);

        assertEquals("admin/users/form", view);
        verify(model).addAttribute("userForm", form);
        verify(model).addAttribute("isEdit", true);
    }

    @Test
    public void updateReturnsFormWhenValidationFailsAndRedirectsWhenValid() {
        UserForm form = new UserForm();
        BindingResult br = mock(BindingResult.class);
        Model model = mock(Model.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(userService.validateBusinessRules(form, br, true)).thenReturn(false);
        assertEquals("admin/users/form", adminUserController.update(10L, form, br, model, redirectAttributes));
        assertEquals(10L, form.getId());
        verify(model).addAttribute("isEdit", true);
        verify(userService, never()).update(any());

        when(userService.validateBusinessRules(form, br, true)).thenReturn(true);
        assertEquals("redirect:/admin/users", adminUserController.update(10L, form, br, model, redirectAttributes));
        verify(userService).update(form);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    public void deleteHandlesSuccessAndIllegalArgumentException() {
        Authentication auth = mock(Authentication.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        when(auth.getName()).thenReturn("admin");

        assertEquals("redirect:/admin/users", adminUserController.delete(2L, auth, redirectAttributes));
        verify(userService).delete(2L, "admin");
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());

        doThrow(new IllegalArgumentException("Erro autoexclusao"))
                .when(userService).delete(1L, "admin");
        assertEquals("redirect:/admin/users", adminUserController.delete(1L, auth, redirectAttributes));
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Erro autoexclusao");
    }
}
