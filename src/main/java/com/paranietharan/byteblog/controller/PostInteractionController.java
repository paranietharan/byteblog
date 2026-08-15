package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.CommentRequest;
import com.paranietharan.byteblog.dto.CommentResponse;
import com.paranietharan.byteblog.dto.LikeResponse;
import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.PostInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Interactions", description = "Comments and idempotent post likes")
public class PostInteractionController {

    private final PostInteractionService interactionService;

    @GetMapping("/api/v1/posts/{postId}/comments")
    @Operation(summary = "List visible comments")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(interactionService.getComments(postId, page, size));
    }

    @PostMapping("/api/v1/posts/{postId}/comments")
    @Operation(summary = "Add a comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return new ResponseEntity<>(
                interactionService.addComment(postId, request, currentUser),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/api/v1/comments/{commentId}")
    @Operation(summary = "Update an owned comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.updateComment(commentId, request, currentUser));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    @Operation(summary = "Delete an owned comment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        interactionService.deleteOwnComment(commentId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully", true));
    }

    @PostMapping("/api/v1/posts/{postId}/like")
    @Operation(summary = "Like a post", description = "Idempotent and protected by a database uniqueness constraint.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LikeResponse> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.likePost(postId, currentUser));
    }

    @DeleteMapping("/api/v1/posts/{postId}/like")
    @Operation(summary = "Unlike a post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<LikeResponse> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.unlikePost(postId, currentUser));
    }
}
