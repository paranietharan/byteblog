package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostComment;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private BlogPostService blogPostService;

    @Mock
    private PostInteractionService interactionService;

    @Mock
    private BlogPostRepository postRepository;

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminModerationService moderationService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        admin.setEmailVerified(true);
    }

    @Test
    void hidePostRecordsModeratorAndTime() {
        BlogPost post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setTitle("Moderated post");
        post.setSlug("moderated-post");
        post.setHidden(false);
        User author = new User();
        author.setId(UUID.randomUUID());
        author.setName("Author");
        author.setEmail("author@example.com");
        post.setAuthor(author);
        when(authenticatedUserService.requireAdmin(admin)).thenReturn(admin);
        when(blogPostService.findPost(post.getId())).thenReturn(post);

        moderationService.hidePost(post.getId(), admin);

        assertTrue(post.getHidden());
        assertTrue(post.getHiddenAt() != null);
        assertTrue(post.getHiddenBy() == admin);
        verify(postRepository).save(post);
        verify(emailService).sendPostModerationNotification(
                author.getEmail(),
                author.getName(),
                post.getTitle(),
                post.getSlug(),
                "hidden"
        );
    }

    @Test
    void unhideCommentClearsModerationMetadata() {
        PostComment comment = new PostComment();
        comment.setId(UUID.randomUUID());
        comment.setHidden(true);
        comment.setHiddenBy(admin);
        when(authenticatedUserService.requireAdmin(admin)).thenReturn(admin);
        when(interactionService.findComment(comment.getId())).thenReturn(comment);

        moderationService.unhideComment(comment.getId(), admin);

        assertFalse(comment.getHidden());
        assertNull(comment.getHiddenAt());
        assertNull(comment.getHiddenBy());
        verify(commentRepository).save(comment);
    }
}
