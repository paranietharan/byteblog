package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.AdminModerationService;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService moderationService;

    @PatchMapping("/posts/{postId}/hide")
    public ResponseEntity<MessageResponse> hidePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.hidePost(postId, currentUser));
    }

    @PatchMapping("/posts/{postId}/unhide")
    public ResponseEntity<MessageResponse> unhidePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.unhidePost(postId, currentUser));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.deletePost(postId, currentUser));
    }

    @PatchMapping("/comments/{commentId}/hide")
    public ResponseEntity<MessageResponse> hideComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.hideComment(commentId, currentUser));
    }

    @PatchMapping("/comments/{commentId}/unhide")
    public ResponseEntity<MessageResponse> unhideComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.unhideComment(commentId, currentUser));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(moderationService.deleteComment(commentId, currentUser));
    }
}
