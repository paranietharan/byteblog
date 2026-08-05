package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.*;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.UserService;
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
@RequestMapping("/users")
@RequiredArgsConstructor
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
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("Get current user profile");
        UUID userId = getCurrentUserId();
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        UUID userId = getCurrentUserId();
        MessageResponse response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/name")
    public ResponseEntity<UserResponse> changeName(
            @Valid @RequestBody ChangeNameRequest request) {
        log.info("Change name request");
        UUID userId = getCurrentUserId();
        UserResponse response = userService.changeName(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/change-request")
    public ResponseEntity<MessageResponse> requestEmailChange(
            @Valid @RequestBody ChangeEmailRequest request) {
        log.info("Email change request");
        UUID userId = getCurrentUserId();
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
