package br.ufscar.pescd.service;

import br.ufscar.pescd.controller.AdminUserApiController;
import br.ufscar.pescd.controller.AdminUserController;
import br.ufscar.pescd.controller.ApiExceptionHandler;
import br.ufscar.pescd.dto.ApiErrorResponse;
import br.ufscar.pescd.dto.PublicUserResponse;
import br.ufscar.pescd.dto.UserForm;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserBlockMud1Test {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
        customUserDetailsService = new CustomUserDetailsService(userRepository);
    }

    private User createUser(Long id, String username, UserRole role, Boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName("Usuario " + username);
        user.setEmail(username + "@ufscar.br");
        user.setUsername(username);
        user.setPassword("encodedPass");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    @Test
    public void toggleBlockStatusBlocksActiveUserAndUnblocksBlockedUser() {
        User target = createUser(2L, "secretario", UserRole.SECRETARIO, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        PublicUserResponse blocked = userService.toggleBlockStatusFromApi(2L, "admin");
        assertFalse(blocked.enabled());
        assertFalse(target.getEnabled());

        PublicUserResponse unblocked = userService.toggleBlockStatusFromApi(2L, "admin");
        assertTrue(unblocked.enabled());
        assertTrue(target.getEnabled());
    }

    @Test
    public void toggleBlockStatusTreatsNullEnabledAsActiveAndBlocks() {
        User target = createUser(3L, "prof", UserRole.PROFESSOR, null);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        User updated = userService.toggleBlockStatus(3L, "admin");
        assertFalse(updated.getEnabled());
    }

    @Test
    public void toggleBlockStatusThrowsWhenAdminAttemptsToBlockSelf() {
        User self = createUser(1L, "admin", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.toggleBlockStatus(1L, "admin");
        });

        assertEquals("Você não pode bloquear ou desbloquear seu próprio usuário.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void updatePreservesBlockedStatusOfExistingUser() {
        User blockedUser = createUser(4L, "prof", UserRole.PROFESSOR, false);
        when(userRepository.findById(4L)).thenReturn(Optional.of(blockedUser));

        UserForm form = new UserForm();
        form.setId(4L);
        form.setFullName("Prof Editado");
        form.setEmail("prof@ufscar.br");
        form.setUsername("prof");
        form.setRole(UserRole.PROFESSOR);

        userService.update(form);

        assertFalse(blockedUser.getEnabled());

        User nullEnabledUser = createUser(5L, "prof2", UserRole.PROFESSOR, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(nullEnabledUser));
        form.setId(5L);
        userService.update(form);
        assertTrue(nullEnabledUser.getEnabled());
    }

    @Test
    public void customUserDetailsServiceMarksBlockedUserAsDisabled() {
        User blockedUser = createUser(2L, "bloqueado", UserRole.ALUNO, false);
        when(userRepository.findByUsername("bloqueado")).thenReturn(Optional.of(blockedUser));

        UserDetails blockedDetails = customUserDetailsService.loadUserByUsername("bloqueado");
        assertFalse(blockedDetails.isEnabled());

        User activeUser = createUser(3L, "ativo", UserRole.ALUNO, true);
        when(userRepository.findByUsername("ativo")).thenReturn(Optional.of(activeUser));

        UserDetails activeDetails = customUserDetailsService.loadUserByUsername("ativo");
        assertTrue(activeDetails.isEnabled());
    }

    @Test
    public void apiExceptionHandlerReturnsMeaningfulMessageForBlockedUser() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        ResponseEntity<ApiErrorResponse> response = handler.handleDisabledOrLockedUser(
                new DisabledException("User is disabled"),
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Usuário bloqueado. Entre em contato com o administrador do sistema.", response.getBody().message());
    }

    @Test
    public void adminControllersDelegateToggleBlockAndHandleSelfBlockError() {
        UserService mockedService = mock(UserService.class);
        AdminUserApiController apiController = new AdminUserApiController(mockedService);
        AdminUserController webController = new AdminUserController(mockedService);
        Authentication auth = mock(Authentication.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);
        when(auth.getName()).thenReturn("admin");

        PublicUserResponse apiExpected = new PublicUserResponse(2L, "Sec", "s@u.br", "sec", "SECRETARIO", false);
        when(mockedService.toggleBlockStatusFromApi(2L, "admin")).thenReturn(apiExpected);
        assertEquals(apiExpected, apiController.toggleBlock(2L, auth));

        User blockedEntity = createUser(2L, "sec", UserRole.SECRETARIO, false);
        when(mockedService.toggleBlockStatus(2L, "admin")).thenReturn(blockedEntity);
        assertEquals("redirect:/admin/users", webController.toggleBlock(2L, auth, redirectAttributes));
        verify(redirectAttributes).addFlashAttribute("successMessage", "Usuário bloqueado com sucesso.");

        User unblockedEntity = createUser(2L, "sec", UserRole.SECRETARIO, true);
        when(mockedService.toggleBlockStatus(2L, "admin")).thenReturn(unblockedEntity);
        assertEquals("redirect:/admin/users", webController.toggleBlock(2L, auth, redirectAttributes));
        verify(redirectAttributes).addFlashAttribute("successMessage", "Usuário desbloqueado com sucesso.");

        when(mockedService.toggleBlockStatus(1L, "admin"))
                .thenThrow(new IllegalArgumentException("Você não pode bloquear ou desbloquear seu próprio usuário."));
        assertEquals("redirect:/admin/users", webController.toggleBlock(1L, auth, redirectAttributes));
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage",
                "Você não pode bloquear ou desbloquear seu próprio usuário."
        );
    }
}
