package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.PostRequest;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostStatus;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import com.paranietharan.byteblog.repository.PostLikeRepository;
import com.paranietharan.byteblog.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceTest {

    @Mock
    private BlogPostRepository postRepository;

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private PostLikeRepository likeRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BlogPostService blogPostService;

    private User author;

    @BeforeEach
    void setUp() {
        author = user(Role.USER);
    }

    @Test
    void createPublishedPostSetsAuthorSlugAndPublishTime() {
        PostRequest request = new PostRequest(
                "Spring Boot & PostgreSQL",
                "A practical guide",
                "# Introduction",
                PostStatus.PUBLISHED
        );
        when(authenticatedUserService.requireVerifiedUser(author)).thenReturn(author);
        when(postRepository.existsBySlug("spring-boot-postgresql")).thenReturn(false);
        when(postRepository.save(any(BlogPost.class))).thenAnswer(invocation -> {
            BlogPost post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });

        var response = blogPostService.createPost(request, author);

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(postRepository).save(captor.capture());
        assertEquals(author.getId(), captor.getValue().getAuthor().getId());
        assertEquals("spring-boot-postgresql", response.getSlug());
        assertEquals(PostStatus.PUBLISHED, response.getStatus());
        assertNotNull(response.getPublishedAt());
        verify(emailService).sendPostPublishedNotification(
                author.getEmail(),
                author.getName(),
                response.getTitle(),
                response.getSlug()
        );
    }

    @Test
    void updatePostRejectsAnotherUser() {
        User anotherUser = user(Role.USER);
        BlogPost post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setAuthor(author);
        when(authenticatedUserService.requireVerifiedUser(anotherUser)).thenReturn(anotherUser);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        PostRequest request = new PostRequest("Updated title", null, "Updated content", PostStatus.DRAFT);

        assertThrows(
                ForbiddenException.class,
                () -> blogPostService.updatePost(post.getId(), request, anotherUser)
        );
    }

    private User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerified(true);
        return user;
    }
}
