package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.AuthorResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.dto.PostRequest;
import com.paranietharan.byteblog.dto.PostResponse;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.Tag;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ConflictException;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import com.paranietharan.byteblog.repository.PostLikeRepository;
import com.paranietharan.byteblog.repository.PostListProjection;
import com.paranietharan.byteblog.repository.PostTagProjection;
import com.paranietharan.byteblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogPostService {

    private final BlogPostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final TagRepository tagRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;

    public PostResponse createPost(PostRequest request, User principal) {
        User author = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = new BlogPost();
        post.setTitle(request.getTitle().trim());
        post.setSlug(generateUniqueSlug(request.getTitle()));
        post.setExcerpt(normalizeOptionalText(request.getExcerpt()));
        post.setContent(request.getContent());
        post.setAuthor(author);
        post.setHidden(false);
        post.setTags(resolveTags(request.getTags()));
        applyPublicationState(post, request, true);

        BlogPost savedPost = postRepository.save(post);
        notifyWhenPublished(savedPost, false);
        return toResponse(savedPost, author);
    }

    public PostResponse updatePost(UUID postId, PostRequest request, User principal) {
        User actor = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = findPost(postId);
        requireOwner(post, actor);
        if (request.getVersion() != null && !request.getVersion().equals(post.getVersion())) {
            throw new ConflictException("Post was modified by another request; reload it before editing");
        }
        boolean wasPublished = post.getStatus() == PostStatus.PUBLISHED;

        post.setTitle(request.getTitle().trim());
        post.setExcerpt(normalizeOptionalText(request.getExcerpt()));
        post.setContent(request.getContent());
        if (request.getTags() != null) {
            post.setTags(resolveTags(request.getTags()));
        }
        applyPublicationState(post, request, false);

        BlogPost savedPost = postRepository.saveAndFlush(post);
        notifyWhenPublished(savedPost, wasPublished);
        return toResponse(savedPost, actor);
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
    public PageResponse<PostResponse> getPublicPosts(
            String query, String tag, int page, int size, User principal) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        String normalizedQuery = normalizeOptionalText(query);
        String normalizedTag = tag == null || tag.isBlank() ? null : slugify(tag, 60);
        UUID viewerId = principal == null ? null : principal.getId();
        Page<PostListProjection> summaries = postRepository.findPublicPostSummaries(
                normalizedQuery,
                normalizedTag,
                viewerId,
                pageable
        );
        Map<UUID, List<String>> tagsByPost = loadTags(
                summaries.getContent().stream().map(PostListProjection::getId).toList()
        );
        return PageResponse.from(summaries.map(summary -> toResponse(summary, tagsByPost.getOrDefault(summary.getId(), List.of()))));
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
        List<String> tags = post.getTags().stream().map(Tag::getName).sorted().toList();

        return PostResponse.builder()
                .id(post.getId())
                .version(post.getVersion())
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
                .tags(tags)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .scheduledPublishAt(post.getScheduledPublishAt())
                .build();
    }

    private PostResponse toResponse(PostListProjection post, List<String> tags) {
        return PostResponse.builder()
                .id(post.getId())
                .version(post.getVersion())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .status(PostStatus.valueOf(post.getStatus()))
                .author(AuthorResponse.builder().id(post.getAuthorId()).name(post.getAuthorName()).build())
                .hidden(post.getHidden())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .likedByCurrentUser(Boolean.TRUE.equals(post.getLikedByCurrentUser()))
                .tags(tags)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .scheduledPublishAt(post.getScheduledPublishAt())
                .build();
    }

    private void applyPublicationState(BlogPost post, PostRequest request, boolean creating) {
        LocalDateTime schedule = request.getScheduledPublishAt();
        if (schedule != null) {
            post.setStatus(PostStatus.DRAFT);
            post.setScheduledPublishAt(schedule);
            return;
        }

        post.setScheduledPublishAt(null);
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        } else if (creating) {
            post.setStatus(PostStatus.DRAFT);
        }
        if (post.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
    }

    private void notifyWhenPublished(BlogPost post, boolean wasPublished) {
        if (!wasPublished && post.getStatus() == PostStatus.PUBLISHED) {
            emailService.sendPostPublishedNotification(
                    post.getAuthor().getEmail(),
                    post.getAuthor().getName(),
                    post.getTitle(),
                    post.getSlug()
            );
        }
    }

    private Set<Tag> resolveTags(Collection<String> requestedTags) {
        if (requestedTags == null || requestedTags.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Map<String, String> uniqueTags = new LinkedHashMap<>();
        for (String rawTag : requestedTags) {
            String name = rawTag.trim();
            String slug = slugify(name, 60);
            if (!slug.isBlank()) {
                uniqueTags.putIfAbsent(slug, name);
            }
        }
        Set<Tag> tags = new LinkedHashSet<>();
        uniqueTags.forEach((slug, name) -> {
            tagRepository.insertIfAbsent(UUID.randomUUID(), name, slug);
            tags.add(tagRepository.findBySlug(slug).orElseThrow());
        });
        return tags;
    }

    private Map<UUID, List<String>> loadTags(List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return tagRepository.findTagsForPosts(postIds).stream()
                .collect(Collectors.groupingBy(
                        PostTagProjection::getPostId,
                        LinkedHashMap::new,
                        Collectors.mapping(PostTagProjection::getTagName, Collectors.toList())
                ));
    }

    private AuthorResponse toAuthorResponse(User author) {
        return AuthorResponse.builder().id(author.getId()).name(author.getName()).build();
    }

    private void requireOwner(BlogPost post, User actor) {
        if (!post.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only manage your own posts");
        }
    }

    private String generateUniqueSlug(String title) {
        String base = slugify(title, 190);
        if (base.isBlank()) {
            base = "post";
        }
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtext(?))", resultSet -> null, base);
        String slug = base;
        while (postRepository.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    private String slugify(String value, int maxLength) {
        String slug = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.length() > maxLength ? slug.substring(0, maxLength).replaceAll("-$", "") : slug;
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
