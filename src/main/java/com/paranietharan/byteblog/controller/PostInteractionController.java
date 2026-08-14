package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.CommentRequest;
import com.paranietharan.byteblog.dto.CommentResponse;
import com.paranietharan.byteblog.dto.LikeResponse;
import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.PostInteractionService;
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
public class PostInteractionController {

    private final PostInteractionService interactionService;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(interactionService.getComments(postId, page, size));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return new ResponseEntity<>(
                interactionService.addComment(postId, request, currentUser),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.updateComment(commentId, request, currentUser));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {
        interactionService.deleteOwnComment(commentId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully", true));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<LikeResponse> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.likePost(postId, currentUser));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<LikeResponse> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interactionService.unlikePost(postId, currentUser));
    }
}
