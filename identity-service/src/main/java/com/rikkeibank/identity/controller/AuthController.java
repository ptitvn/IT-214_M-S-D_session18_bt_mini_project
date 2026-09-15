package com.rikkeibank.identity.controller;

import com.rikkeibank.identity.dto.AuthDtos.*;
import com.rikkeibank.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("X-User-Name") String username) {
        authService.logout(username);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /** ADMIN only — enforced downstream via X-User-Role header set by the Gateway. */
    @PostMapping("/force-logout")
    public ResponseEntity<MessageResponse> forceLogout(@Valid @RequestBody ForceLogoutRequest request,
                                                        @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(new MessageResponse("Only ADMIN can force logout a user"));
        }
        authService.forceLogout(request.username());
        return ResponseEntity.ok(new MessageResponse("User " + request.username() + " has been revoked and force-logged-out"));
    }
}
