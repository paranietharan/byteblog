package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.AuthorResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.dto.PostRequest;
import com.paranietharan.byteblog.dto.PostResponse;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import com.paranietharan.byteblog.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogPostService {

    private final BlogPostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public PostResponse createPost(PostRequest request, User principal) {
        User author = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle().trim());
        post.setSlug(generateUniqueSlug(request.getTitle()));
        post.setExcerpt(normalizeOptionalText(request.getExcerpt()));
        post.setContent(request.getContent());
        post.setStatus(request.getStatus() == null ? PostStatus.DRAFT : request.getStatus());
        post.setAuthor(author);
        post.setHidden(false);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(postRepository.save(post), author);
    }

    public PostResponse updatePost(UUID postId, PostRequest request, User principal) {
        User actor = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = findPost(postId);
        requireOwner(post, actor);

        post.setTitle(request.getTitle().trim());
        post.setExcerpt(normalizeOptionalText(request.getExcerpt()));
        post.setContent(request.getContent());
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }
        if (post.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(postRepository.save(post), actor);
    }

    public void deleteOwnPost(UUID postId, User principal) {
        User actor = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = findPost(postId);
        requireOwner(post, actor);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPublicPost(String slug, User principal) {
        BlogPost post = postRepository.findBySlugAndStatusAndHiddenFalse(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return toResponse(post, principal);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPublicPosts(String query, int page, int size, User principal) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<PostResponse> posts = postRepository
                .findPublicPosts(PostStatus.PUBLISHED, normalizedQuery, pageable)
                .map(post -> toResponse(post, principal));
        return PageResponse.from(posts);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getMyPosts(int page, int size, User principal) {
        User author = authenticatedUserService.requireVerifiedUser(principal);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<PostResponse> posts = postRepository.findByAuthorOrderByCreatedAtDesc(author, pageable)
                .map(post -> toResponse(post, author));
        return PageResponse.from(posts);
    }

    @Transactional(readOnly = true)
    public BlogPost requirePublicPost(UUID postId) {
        return postRepository.findByIdAndStatusAndHiddenFalse(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    @Transactional(readOnly = true)
    public BlogPost findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    public PostResponse toResponse(BlogPost post, User viewer) {
        boolean liked = viewer != null
                && viewer.getId() != null
                && likeRepository.existsByPostAndUser(post, viewer);

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .status(post.getStatus())
                .author(toAuthorResponse(post.getAuthor()))
                .hidden(post.getHidden())
                .likeCount(likeRepository.countByPostId(post.getId()))
                .commentCount(commentRepository.countByPostIdAndHiddenFalse(post.getId()))
                .likedByCurrentUser(liked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .build();
    }

    private AuthorResponse toAuthorResponse(User author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .build();
    }

    private void requireOwner(BlogPost post, User actor) {
        if (!post.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only manage your own posts");
        }
    }

    private String generateUniqueSlug(String title) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "post";
        }
        if (base.length() > 190) {
            base = base.substring(0, 190).replaceAll("-$", "");
        }

        String slug = base;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
