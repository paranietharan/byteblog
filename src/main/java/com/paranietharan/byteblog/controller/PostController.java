package com.paranietharan.byteblog.controller;

import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.dto.PostRequest;
import com.paranietharan.byteblog.dto.PostResponse;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.service.BlogPostService;
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
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<PageResponse<PostResponse>> getPosts(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getPublicPosts(query, page, size, currentUser));
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getMyPosts(page, size, currentUser));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String slug,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.getPublicPost(slug, currentUser));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal User currentUser) {
        return new ResponseEntity<>(blogPostService.createPost(request, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(blogPostService.updatePost(postId, request, currentUser));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User currentUser) {
        blogPostService.deleteOwnPost(postId, currentUser);
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully", true));
    }
}
