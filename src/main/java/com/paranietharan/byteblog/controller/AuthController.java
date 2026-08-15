package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, email verification, login, token rotation, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register an account", description = "Creates an account and queues an email-verification message. Rate limited.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a verified, active account and issues access and refresh tokens. Rate limited.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify an email address", description = "Consumes the one-time verification token delivered by email. Rate limited.")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        log.info("Email verification request");
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate a refresh token", description = "Revokes the supplied token and returns a new token in the same family. Reuse revokes the entire family. Rate limited.")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        AuthResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Revokes the supplied refresh token.")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        MessageResponse response = authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate an access token", description = "Requires a valid bearer access token.")
    public ResponseEntity<MessageResponse> validateToken() {
        return ResponseEntity.ok(new MessageResponse("Token is valid", true));
    }
}
