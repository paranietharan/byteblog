package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostLike;
import com.paranietharan.byteblog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    boolean existsByPostAndUser(BlogPost post, User user);

    long deleteByPostAndUser(BlogPost post, User user);

    long countByPostId(UUID postId);
}
