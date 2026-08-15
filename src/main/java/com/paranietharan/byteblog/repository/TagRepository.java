package com.paranietharan.byteblog.repository;

import com.paranietharan.byteblog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);

    @Modifying
    @Query(value = """
            INSERT INTO tags (id, name, slug, created_at)
            VALUES (:id, :name, :slug, CURRENT_TIMESTAMP)
            ON CONFLICT (slug) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("name") String name, @Param("slug") String slug);

    @Query(value = """
            SELECT mapping.post_id AS "postId", tag.name AS "tagName"
            FROM post_tags mapping
            JOIN tags tag ON tag.id = mapping.tag_id
            WHERE mapping.post_id IN (:postIds)
            ORDER BY tag.name
            """, nativeQuery = true)
    List<PostTagProjection> findTagsForPosts(@Param("postIds") Collection<UUID> postIds);
}
