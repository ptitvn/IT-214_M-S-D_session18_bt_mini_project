package com.rikkeibank.identity.dto;

import com.rikkeibank.identity.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RegisterRequest(@NotBlank String username, @NotBlank String password,
                                   @NotNull Role role, Long referenceId) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ForceLogoutRequest(@NotBlank String username) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                 long expiresInMs, String username, Role role, Long referenceId) {}

    public record MessageResponse(String message) {}
}
