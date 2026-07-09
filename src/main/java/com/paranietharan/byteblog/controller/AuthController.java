package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        log.info("Email verification request");
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        AuthResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        MessageResponse response = authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<MessageResponse> validateToken() {
        return ResponseEntity.ok(new MessageResponse("Token is valid", true));
    }
}
