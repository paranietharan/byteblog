package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Extract user ID from authentication - you may need to modify this based on your implementation
            // For now, we'll use a header or modify the authentication principal
            String email = userDetails.getUsername();
            // This is a placeholder - in real implementation, you'd store user ID in token or context
            return extractUserIdFromContext();
        }
        throw new IllegalArgumentException("User not authenticated");
    }

    private Long extractUserIdFromContext() {
        // This should be extracted from JWT token or SecurityContext
        // For now, returning a placeholder that will be replaced with proper implementation
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // In production, implement proper user ID extraction
        return null;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("Get current user profile");
        Long userId = getCurrentUserId();
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        Long userId = getCurrentUserId();
        MessageResponse response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/name")
    public ResponseEntity<UserResponse> changeName(
            @Valid @RequestBody ChangeNameRequest request) {
        log.info("Change name request");
        Long userId = getCurrentUserId();
        UserResponse response = userService.changeName(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/change-request")
    public ResponseEntity<MessageResponse> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequest request) {
        log.info("Email change request");
        Long userId = getCurrentUserId();
        MessageResponse response = userService.requestEmailChange(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/verify-change")
    public ResponseEntity<MessageResponse> verifyEmailChange(@RequestParam String token) {
        log.info("Email change verification");
        MessageResponse response = userService.verifyEmailChange(token);
        return ResponseEntity.ok(response);
    }
}
