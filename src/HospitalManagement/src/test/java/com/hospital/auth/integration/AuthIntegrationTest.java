package com.hospital.auth.integration;

import com.hospital.auth.presentation.dto.AuthResponseDto;
import com.hospital.auth.presentation.dto.LoginRequest;
import com.hospital.auth.presentation.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.docker.compose.enabled=false"
)
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_shouldReturn201WithTokens_whenEmailIsNew() {
        RegisterRequest request = new RegisterRequest("newuser@hospital.com", "password123");

        ResponseEntity<AuthResponseDto> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void register_shouldReturn409_whenEmailIsAlreadyTaken() {
        RegisterRequest request = new RegisterRequest("duplicate@hospital.com", "password123");
        restTemplate.postForEntity("/api/auth/register", request, AuthResponseDto.class);

        ResponseEntity<String> secondAttempt = restTemplate.postForEntity(
                "/api/auth/register", request, String.class);

        assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturn200WithTokens_whenCredentialsAreValid() {
        String email = "loginuser@hospital.com";
        String password = "password123";
        restTemplate.postForEntity("/api/auth/register", new RegisterRequest(email, password), AuthResponseDto.class);

        ResponseEntity<AuthResponseDto> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, password), AuthResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
    }

    @Test
    void login_shouldReturn401_whenPasswordIsWrong() {
        String email = "wrongpass@hospital.com";
        restTemplate.postForEntity("/api/auth/register", new RegisterRequest(email, "password123"), AuthResponseDto.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "wrongpassword"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_shouldReturn401_whenUserDoesNotExist() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("ghost@hospital.com", "password123"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── JWT protection ────────────────────────────────────────────────────────

    @Test
    void protectedEndpoint_shouldReturn401_whenNoTokenProvided() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/appointments", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_shouldNotReturn401_whenValidTokenProvided() {
        String email = "tokenuser@hospital.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "password123");
        AuthResponseDto auth = restTemplate.postForEntity(
                "/api/auth/register", registerRequest, AuthResponseDto.class).getBody();

        assertThat(auth).isNotNull();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.accessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/appointments", HttpMethod.GET, request, String.class);

        // 404 is fine — endpoint doesn't exist yet, but NOT 401
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
