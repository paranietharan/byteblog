package com.paranietharan.byteblog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.paranietharan.byteblog.entity.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private UUID id;
    private Long version;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private PostStatus status;
    private AuthorResponse author;
    private Boolean hidden;
    private long likeCount;
    private long commentCount;
    private boolean likedByCurrentUser;
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledPublishAt;
}
