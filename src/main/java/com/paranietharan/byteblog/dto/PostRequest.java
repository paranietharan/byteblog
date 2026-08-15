package com.paranietharan.byteblog.dto;

import com.paranietharan.byteblog.entity.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    public PostRequest(String title, String excerpt, String content, PostStatus status) {
        this.title = title;
        this.excerpt = excerpt;
        this.content = content;
        this.status = status;
    }

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private PostStatus status;

    @Future(message = "Scheduled publish time must be in the future")
    private LocalDateTime scheduledPublishAt;

    @Size(max = 5, message = "A post can have at most 5 tags")
    private List<@NotBlank(message = "Tag must not be blank") @Size(max = 50, message = "Tag must not exceed 50 characters") String> tags;

    private Long version;
}
