package com.hospital.auth.application;

import com.hospital.auth.domain.EmailAlreadyExistsException;
import com.hospital.auth.domain.Role;
import com.hospital.auth.domain.User;
import com.hospital.auth.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_shouldSaveUserWithEncodedPassword_whenEmailIsNew() {
        when(userRepository.existsByEmail("new@hospital.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register("new@hospital.com", "raw-password", Role.PATIENT);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@hospital.com")
                && u.getPassword().equals("encoded-password")
                && u.getRole() == Role.PATIENT
                && u.isActive()
        ));
    }

    @Test
    void register_shouldThrowEmailAlreadyExistsException_whenEmailIsTaken() {
        when(userRepository.existsByEmail("taken@hospital.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("taken@hospital.com", "pass", Role.PATIENT))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("taken@hospital.com");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        User storedUser = buildActiveUser("doctor@hospital.com", "encoded-pass", Role.DOCTOR);
        when(userRepository.findByEmail("doctor@hospital.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-pass", "encoded-pass")).thenReturn(true);
        when(jwtService.generateAccessToken(storedUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(storedUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login("doctor@hospital.com", "raw-pass");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@hospital.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@hospital.com", "any-pass"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenPasswordIsWrong() {
        User storedUser = buildActiveUser("patient@hospital.com", "encoded-pass", Role.PATIENT);
        when(userRepository.findByEmail("patient@hospital.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrong-pass", "encoded-pass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("patient@hospital.com", "wrong-pass"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenUserIsInactive() {
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@hospital.com")
                .password("encoded-pass")
                .role(Role.PATIENT)
                .active(false)
                .build();
        when(userRepository.findByEmail("inactive@hospital.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("raw-pass", "encoded-pass")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("inactive@hospital.com", "raw-pass"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildActiveUser(String email, String encodedPassword, Role role) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(encodedPassword)
                .role(role)
                .active(true)
                .build();
    }
}
