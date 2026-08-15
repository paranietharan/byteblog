package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostLike;
import com.paranietharan.byteblog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    boolean existsByPostAndUser(BlogPost post, User user);

    long deleteByPostAndUser(BlogPost post, User user);

    long countByPostId(UUID postId);

    @Modifying
    @Query(value = """
            INSERT INTO post_likes (id, post_id, user_id, created_at)
            VALUES (:id, :postId, :userId, CURRENT_TIMESTAMP)
            ON CONFLICT (post_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("postId") UUID postId, @Param("userId") UUID userId);
}
