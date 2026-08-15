package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    boolean existsBySlug(String slug);

    Optional<BlogPost> findBySlugAndStatusAndHiddenFalse(String slug, PostStatus status);

    Optional<BlogPost> findByIdAndStatusAndHiddenFalse(UUID id, PostStatus status);

    Page<BlogPost> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    @Query(value = """
            SELECT post.id AS id,
                   post.version AS version,
                   post.title AS title,
                   post.slug AS slug,
                   post.excerpt AS excerpt,
                   post.content AS content,
                   post.status AS status,
                   author.id AS "authorId",
                   author.name AS "authorName",
                   post.hidden AS hidden,
                   (SELECT COUNT(*) FROM post_likes likes WHERE likes.post_id = post.id) AS "likeCount",
                   (SELECT COUNT(*) FROM post_comments comments
                    WHERE comments.post_id = post.id AND comments.hidden = FALSE) AS "commentCount",
                   (CAST(:viewerId AS UUID) IS NOT NULL AND EXISTS (
                       SELECT 1 FROM post_likes viewer_like
                       WHERE viewer_like.post_id = post.id AND viewer_like.user_id = CAST(:viewerId AS UUID)
                   )) AS "likedByCurrentUser",
                   post.created_at AS "createdAt",
                   post.updated_at AS "updatedAt",
                   post.published_at AS "publishedAt",
                   post.scheduled_publish_at AS "scheduledPublishAt"
            FROM blog_posts post
            JOIN users author ON author.id = post.author_id
            WHERE post.status = 'PUBLISHED'
              AND post.hidden = FALSE
              AND (:query IS NULL OR post.search_vector @@ websearch_to_tsquery('english', :query))
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM post_tags mapping
                  JOIN tags tag ON tag.id = mapping.tag_id
                  WHERE mapping.post_id = post.id AND tag.slug = :tag
              ))
            ORDER BY post.published_at DESC, post.created_at DESC
            """, countQuery = """
            SELECT COUNT(*) FROM blog_posts post
            WHERE post.status = 'PUBLISHED'
              AND post.hidden = FALSE
              AND (:query IS NULL OR post.search_vector @@ websearch_to_tsquery('english', :query))
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM post_tags mapping
                  JOIN tags tag ON tag.id = mapping.tag_id
                  WHERE mapping.post_id = post.id AND tag.slug = :tag
              ))
            """, nativeQuery = true)
    Page<PostListProjection> findPublicPostSummaries(
            @Param("query") String query,
            @Param("tag") String tag,
            @Param("viewerId") UUID viewerId,
            Pageable pageable
    );

    @Query(value = """
            SELECT * FROM blog_posts
            WHERE status = 'DRAFT'
              AND hidden = FALSE
              AND scheduled_publish_at IS NOT NULL
              AND scheduled_publish_at <= :now
            ORDER BY scheduled_publish_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<BlogPost> findDueForPublishing(
            @Param("now") java.time.LocalDateTime now,
            @Param("limit") int limit
    );
}
