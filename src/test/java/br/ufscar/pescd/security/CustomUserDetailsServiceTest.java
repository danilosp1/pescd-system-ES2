package br.ufscar.pescd.security;

import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        customUserDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    public void loadUserByUsernameReturnsUserDetailsWhenFound() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("encodedPass");
        user.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("encodedPass", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    public void loadUserByUsernameThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("inexistente");
        });
    }
}
