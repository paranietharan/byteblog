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
import java.util.UUID;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    boolean existsBySlug(String slug);

    Optional<BlogPost> findBySlugAndStatusAndHiddenFalse(String slug, PostStatus status);

    Optional<BlogPost> findByIdAndStatusAndHiddenFalse(UUID id, PostStatus status);

    Page<BlogPost> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    @Query("""
            SELECT post FROM BlogPost post
            WHERE post.status = :status
              AND post.hidden = false
              AND (:query IS NULL
                   OR LOWER(post.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(post.excerpt) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY post.publishedAt DESC, post.createdAt DESC
            """)
    Page<BlogPost> findPublicPosts(
            @Param("status") PostStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}
