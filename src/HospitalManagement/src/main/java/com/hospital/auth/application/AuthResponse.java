package com.hospital.auth.application;

public record AuthResponse(String accessToken, String refreshToken) {
}
