package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.AdminModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin moderation", description = "Verified administrator hide, restore, and delete operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminModerationController {

    private final AdminModerationService moderationService;

    @PatchMapping("/posts/{postId}/hide")
    @Operation(summary = "Hide a post")
    public ResponseEntity<MessageResponse> hidePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.hidePost(postId, currentUser));
    }

    @PatchMapping("/posts/{postId}/unhide")
    @Operation(summary = "Restore a post")
    public ResponseEntity<MessageResponse> unhidePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.unhidePost(postId, currentUser));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Permanently delete a post")
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.deletePost(postId, currentUser));
    }

    @PatchMapping("/comments/{commentId}/hide")
    @Operation(summary = "Hide a comment")
    public ResponseEntity<MessageResponse> hideComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.hideComment(commentId, currentUser));
    }

    @PatchMapping("/comments/{commentId}/unhide")
    @Operation(summary = "Restore a comment")
    public ResponseEntity<MessageResponse> unhideComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.unhideComment(commentId, currentUser));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Permanently delete a comment")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.deleteComment(commentId, currentUser));
    }
}
