package com.financeflow.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, String tokenType) {
    public static AuthResponse bearer(String a, String r) { return new AuthResponse(a, r, "Bearer"); }
}