package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledPublishingService {

    private final BlogPostRepository postRepository;
    private final EmailService emailService;

    @Value("${app.posts.scheduled-publish-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.posts.scheduled-publish-delay-ms:30000}")
    @Transactional
    public void publishDuePosts() {
        List<BlogPost> posts = postRepository.findDueForPublishing(LocalDateTime.now(), batchSize);
        for (BlogPost post : posts) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
            post.setScheduledPublishAt(null);
            postRepository.save(post);
            emailService.sendPostPublishedNotification(
                    post.getAuthor().getEmail(),
                    post.getAuthor().getName(),
                    post.getTitle(),
                    post.getSlug()
            );
        }
    }
}
