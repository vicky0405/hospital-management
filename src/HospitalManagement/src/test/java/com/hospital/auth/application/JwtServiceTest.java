package com.hospital.auth.application;

import com.hospital.auth.domain.Role;
import com.hospital.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1ob3NwaXRhbC1tYW5hZ2VtZW50LXByb2plY3Q=";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 3_600_000L;  // 1h
    private static final long REFRESH_TOKEN_EXPIRY_MS = 604_800_000L; // 7d

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_TOKEN_EXPIRY_MS, REFRESH_TOKEN_EXPIRY_MS);
    }

    @Test
    void generateAccessToken_shouldReturnNonNullToken_whenUserIsValid() {
        User user = buildUser();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateRefreshToken_shouldReturnNonNullToken_whenUserIsValid() {
        User user = buildUser();

        String token = jwtService.generateRefreshToken(user);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail_whenTokenIsValid() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("patient@hospital.com");
    }

    @Test
    void extractRole_shouldReturnCorrectRole_whenTokenIsValid() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);

        Role role = jwtService.extractRole(token);

        assertThat(role).isEqualTo(Role.PATIENT);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenMatchesActiveUser() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);
        UserDetails userDetails = buildUserDetails("patient@hospital.com", true);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenEmailDoesNotMatch() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);
        UserDetails anotherUserDetails = buildUserDetails("other@hospital.com", true);

        boolean valid = jwtService.isTokenValid(token, anotherUserDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUserIsInactive() {
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .email("patient@hospital.com")
                .role(Role.PATIENT)
                .active(false)
                .build();
        String token = jwtService.generateAccessToken(inactiveUser);
        UserDetails inactiveDetails = buildUserDetails("patient@hospital.com", false);

        boolean valid = jwtService.isTokenValid(token, inactiveDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L, REFRESH_TOKEN_EXPIRY_MS);
        User user = buildUser();
        String token = shortLivedJwtService.generateAccessToken(user);
        UserDetails userDetails = buildUserDetails("patient@hospital.com", true);

        Thread.sleep(100);

        boolean valid = shortLivedJwtService.isTokenValid(token, userDetails);

        assertThat(valid).isFalse();
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("patient@hospital.com")
                .role(Role.PATIENT)
                .active(true)
                .build();
    }

    private UserDetails buildUserDetails(String email, boolean enabled) {
        return new org.springframework.security.core.userdetails.User(
                email, "encoded-password", enabled, true, true, true, List.of()
        );
    }
}
