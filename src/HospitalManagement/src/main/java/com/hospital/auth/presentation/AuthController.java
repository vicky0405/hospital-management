package com.hospital.auth.presentation;

import com.hospital.auth.application.AuthResponse;
import com.hospital.auth.application.AuthService;
import com.hospital.auth.domain.Role;
import com.hospital.auth.presentation.dto.AuthResponseDto;
import com.hospital.auth.presentation.dto.LoginRequest;
import com.hospital.auth.presentation.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponseDto register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request.email(), request.password(), Role.PATIENT);
        return new AuthResponseDto(response.accessToken(), response.refreshToken());
    }

    @PostMapping("/login")
    AuthResponseDto login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.email(), request.password());
        return new AuthResponseDto(response.accessToken(), response.refreshToken());
    }
}
