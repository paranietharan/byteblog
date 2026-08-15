package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.dto.PostRequest;
import com.paranietharan.byteblog.dto.PostResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.BlogPostService;
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
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Public discovery and verified-author publishing")
public class PostController {

    private final BlogPostService blogPostService;

    @GetMapping
    @Operation(summary = "List published posts", description = "Supports PostgreSQL full-text search, tag filtering, and pagination.")
    public ResponseEntity<PageResponse<PostResponse>> getPosts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getPublicPosts(query, tag, page, size, currentUser));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current author's posts", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getMyPosts(page, size, currentUser));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a published post by slug")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String slug,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getPublicPost(slug, currentUser));
    }

    @PostMapping
    @Operation(summary = "Create a draft, scheduled, or published post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal User currentUser) {
        return new ResponseEntity<>(blogPostService.createPost(request, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update an owned post", description = "Uses optimistic locking to reject conflicting edits.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.updatePost(postId, request, currentUser));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete an owned post", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        blogPostService.deleteOwnPost(postId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully", true));
    }
}
