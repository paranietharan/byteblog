package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PostQueryEfficiencyIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogPostRepository postRepository;

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void publicPostPageUsesConstantQueryCount() {
        User author = new User();
        author.setName("Query Test Author");
        author.setEmail("query-" + UUID.randomUUID() + "@example.com");
        author.setPassword("hashed-password");
        author.setRole(Role.USER);
        author.setActive(true);
        author.setEmailVerified(true);
        author = userRepository.saveAndFlush(author);

        for (int index = 0; index < 20; index++) {
            BlogPost post = new BlogPost();
            post.setTitle("Query test " + index);
            post.setSlug("query-test-" + UUID.randomUUID());
            post.setContent("Full text content " + index);
            post.setStatus(PostStatus.PUBLISHED);
            post.setAuthor(author);
            post.setHidden(false);
            post.setPublishedAt(LocalDateTime.now());
            postRepository.save(post);
        }
        postRepository.flush();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        var response = blogPostService.getPublicPosts("Full text", null, 0, 20, null);

        assertEquals(20, response.getContent().size());
        assertTrue(statistics.getPrepareStatementCount() <= 3,
                "Expected at most page, count, and batched-tag queries but got " + statistics.getPrepareStatementCount());
    }
}
