package com.paranietharan.byteblog.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PostListProjection {
    UUID getId();
    Long getVersion();
    String getTitle();
    String getSlug();
    String getExcerpt();
    String getContent();
    String getStatus();
    UUID getAuthorId();
    String getAuthorName();
    Boolean getHidden();
    Long getLikeCount();
    Long getCommentCount();
    Boolean getLikedByCurrentUser();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getPublishedAt();
    LocalDateTime getScheduledPublishAt();
}
