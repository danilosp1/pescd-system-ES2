package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.PublicUserResponse;
import br.ufscar.pescd.dto.UserForm;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.exception.DuplicateResourceException;
import br.ufscar.pescd.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    private User createSampleUser(Long id, String username, UserRole role, Boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName("Usuario Teste");
        user.setEmail(username + "@ufscar.br");
        user.setUsername(username);
        user.setPassword("encodedPass");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    private UserForm createValidForm() {
        UserForm form = new UserForm();
        form.setFullName("Novo Usuario");
        form.setEmail("novo@ufscar.br");
        form.setUsername("novousuario");
        form.setPassword("123456");
        form.setRole(UserRole.PROFESSOR);
        return form;
    }

    @Test
    public void findAllReturnsOrderedUsers() {
        User user = createSampleUser(1L, "admin", UserRole.ADMIN, true);
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(user));

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUsername());
        verify(userRepository).findAllByOrderByFullNameAsc();
    }

    @Test
    public void findAllPublicMapsUsersToPublicResponse() {
        User enabledUser = createSampleUser(1L, "admin", UserRole.ADMIN, true);
        User disabledUser = createSampleUser(2L, "prof", UserRole.PROFESSOR, false);
        when(userRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(enabledUser, disabledUser));

        List<PublicUserResponse> result = userService.findAllPublic();

        assertEquals(2, result.size());
        assertTrue(result.get(0).enabled());
        assertFalse(result.get(1).enabled());
    }

    @Test
    public void findByIdReturnsUserWhenPresent() {
        User user = createSampleUser(1L, "admin", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertSame(user, result);
    }

    @Test
    public void findByIdThrowsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            userService.findById(99L);
        });
    }

    @Test
    public void findPublicByIdReturnsPublicUserResponse() {
        User user = createSampleUser(1L, "secretario", UserRole.SECRETARIO, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PublicUserResponse response = userService.findPublicById(1L);

        assertEquals(1L, response.id());
        assertEquals("secretario", response.username());
        assertEquals("SECRETARIO", response.role());
    }

    @Test
    public void toFormMapsEntityFields() {
        User user = createSampleUser(5L, "prof", UserRole.PROFESSOR, true);

        UserForm form = userService.toForm(user);

        assertEquals(5L, form.getId());
        assertEquals(user.getFullName(), form.getFullName());
        assertEquals(user.getEmail(), form.getEmail());
        assertEquals(user.getUsername(), form.getUsername());
        assertEquals(UserRole.PROFESSOR, form.getRole());
    }

    @Test
    public void validateBusinessRulesReturnsTrueWhenValidForCreate() {
        UserForm form = createValidForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        boolean valid = userService.validateBusinessRules(form, bindingResult, false);

        assertTrue(valid);
        assertFalse(bindingResult.hasErrors());
    }

    @Test
    public void validateBusinessRulesRejectsDuplicatesBlankPasswordAndAlunoOnCreate() {
        UserForm form = createValidForm();
        form.setPassword("   ");
        form.setRole(UserRole.ALUNO);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        when(userRepository.existsByEmail(form.getEmail())).thenReturn(true);
        when(userRepository.existsByUsername(form.getUsername())).thenReturn(true);

        boolean valid = userService.validateBusinessRules(form, bindingResult, false);

        assertFalse(valid);
        assertNotNull(bindingResult.getFieldError("email"));
        assertNotNull(bindingResult.getFieldError("username"));
        assertNotNull(bindingResult.getFieldError("password"));
        assertNotNull(bindingResult.getFieldError("role"));
    }

    @Test
    public void validateBusinessRulesRejectsNullAndShortPasswordOnCreate() {
        UserForm formNullPass = createValidForm();
        formNullPass.setPassword(null);
        BindingResult brNull = new BeanPropertyBindingResult(formNullPass, "userForm");

        assertFalse(userService.validateBusinessRules(formNullPass, brNull, false));
        assertNotNull(brNull.getFieldError("password"));

        UserForm formShortPass = createValidForm();
        formShortPass.setPassword("123");
        BindingResult brShort = new BeanPropertyBindingResult(formShortPass, "userForm");

        assertFalse(userService.validateBusinessRules(formShortPass, brShort, false));
        assertNotNull(brShort.getFieldError("password"));
    }

    @Test
    public void validateBusinessRulesOnEditChecksDuplicatesWithIdNotAndAllowsNullPassword() {
        UserForm form = createValidForm();
        form.setId(10L);
        form.setPassword(null);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        when(userRepository.existsByEmailAndIdNot(form.getEmail(), 10L)).thenReturn(true);
        when(userRepository.existsByUsernameAndIdNot(form.getUsername(), 10L)).thenReturn(true);

        boolean valid = userService.validateBusinessRules(form, bindingResult, true);

        assertFalse(valid);
        assertNotNull(bindingResult.getFieldError("email"));
        assertNotNull(bindingResult.getFieldError("username"));
        assertNull(bindingResult.getFieldError("password"));
    }

    @Test
    public void validateBusinessRulesOnEditPassesWhenNoDuplicates() {
        UserForm form = createValidForm();
        form.setId(10L);
        form.setPassword("");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userForm");

        when(userRepository.existsByEmailAndIdNot(form.getEmail(), 10L)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot(form.getUsername(), 10L)).thenReturn(false);

        assertTrue(userService.validateBusinessRules(form, bindingResult, true));
    }

    @Test
    public void createEncodesPasswordAndSavesUser() {
        UserForm form = createValidForm();
        when(passwordEncoder.encode("123456")).thenReturn("hashed123456");

        userService.create(form);

        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void createFromApiSavesAndReturnsPublicUserResponse() {
        UserForm form = createValidForm();
        User saved = createSampleUser(7L, form.getUsername(), form.getRole(), true);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        PublicUserResponse response = userService.createFromApi(form);

        assertEquals(7L, response.id());
        assertEquals(form.getUsername(), response.username());
    }

    @Test
    public void createFromApiThrowsOnDuplicateEmailOrUsername() {
        UserForm form = createValidForm();
        when(userRepository.existsByEmail(form.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.createFromApi(form);
        });

        when(userRepository.existsByEmail(form.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(form.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.createFromApi(form);
        });
    }

    @Test
    public void createFromApiThrowsOnInvalidPasswordsOrAlunoRole() {
        UserForm formNullPass = createValidForm();
        formNullPass.setPassword(null);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createFromApi(formNullPass);
        });

        UserForm formBlankPass = createValidForm();
        formBlankPass.setPassword("  ");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createFromApi(formBlankPass);
        });

        UserForm formShortPass = createValidForm();
        formShortPass.setPassword("12345");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createFromApi(formShortPass);
        });

        UserForm formAluno = createValidForm();
        formAluno.setRole(UserRole.ALUNO);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createFromApi(formAluno);
        });
    }

    @Test
    public void updateKeepsExistingPasswordWhenFormPasswordIsBlankOrNull() {
        User existing = createSampleUser(3L, "prof", UserRole.PROFESSOR, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));

        UserForm form = createValidForm();
        form.setId(3L);
        form.setPassword("   ");

        userService.update(form);

        assertEquals("encodedPass", existing.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(existing);

        form.setPassword(null);
        userService.update(form);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    public void updateEncodesNewPasswordWhenProvided() {
        User existing = createSampleUser(3L, "prof", UserRole.PROFESSOR, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("newEncoded");

        UserForm form = createValidForm();
        form.setId(3L);
        form.setPassword("novaSenha123");

        userService.update(form);

        assertEquals("newEncoded", existing.getPassword());
        verify(userRepository).save(existing);
    }

    @Test
    public void updateFromApiValidatesAndUpdatesUser() {
        User existing = createSampleUser(4L, "prof", UserRole.PROFESSOR, true);
        when(userRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserForm form = createValidForm();
        form.setId(4L);
        form.setPassword("");

        PublicUserResponse response = userService.updateFromApi(form);

        assertEquals(4L, response.id());
        verify(userRepository).save(existing);
    }

    @Test
    public void updateFromApiThrowsWhenDuplicateEmailOrUsernameOnEdit() {
        UserForm form = createValidForm();
        form.setId(4L);

        when(userRepository.existsByEmailAndIdNot(form.getEmail(), 4L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> {
            userService.updateFromApi(form);
        });

        when(userRepository.existsByEmailAndIdNot(form.getEmail(), 4L)).thenReturn(false);
        when(userRepository.existsByUsernameAndIdNot(form.getUsername(), 4L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> {
            userService.updateFromApi(form);
        });
    }

    @Test
    public void deleteRemovesUserWhenDifferentFromLoggedUser() {
        User target = createSampleUser(2L, "secretario", UserRole.SECRETARIO, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userService.delete(2L, "admin");

        verify(userRepository).delete(target);
    }

    @Test
    public void deleteThrowsWhenTryingToDeleteOwnUser() {
        User self = createSampleUser(1L, "admin", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThrows(IllegalArgumentException.class, () -> {
            userService.delete(1L, "admin");
        });

        verify(userRepository, never()).delete(any());
    }
}
