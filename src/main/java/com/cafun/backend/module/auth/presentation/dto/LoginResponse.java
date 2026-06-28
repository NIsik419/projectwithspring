package com.cafun.backend.module.auth.presentation.dto;

public record LoginResponse(String tokenType, String accessToken) {
    public static LoginResponse of(String token) {
        return new LoginResponse("Bearer", token);
    }
}
