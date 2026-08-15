package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current-user profile and credential management")
public class UserController {

    private final UserService userService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalArgumentException("User not authenticated");
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("Get current user profile");
        UUID userId = getCurrentUserId();
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    @Operation(summary = "Change the current password", description = "Revokes every active refresh-token family.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        UUID userId = getCurrentUserId();
        MessageResponse response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/name")
    @Operation(summary = "Change the display name", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> changeName(
            @Valid @RequestBody ChangeNameRequest request) {
        log.info("Change name request");
        UUID userId = getCurrentUserId();
        UserResponse response = userService.changeName(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/change-request")
    @Operation(summary = "Request an email change", description = "Queues a verification message for the new address.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequest request) {
        log.info("Email change request");
        UUID userId = getCurrentUserId();
        MessageResponse response = userService.requestEmailChange(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/verify-change")
    @Operation(summary = "Verify an email change", description = "Consumes a one-time email-change token and revokes existing refresh tokens.")
    public ResponseEntity<MessageResponse> verifyEmailChange(@RequestParam String token) {
        log.info("Email change verification");
        MessageResponse response = userService.verifyEmailChange(token);
        return ResponseEntity.ok(response);
    }
}
