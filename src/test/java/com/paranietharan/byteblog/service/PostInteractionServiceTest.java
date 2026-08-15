package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.CommentRequest;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostComment;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import com.paranietharan.byteblog.repository.PostLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostInteractionServiceTest {

    @Mock
    private BlogPostService blogPostService;

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private PostLikeRepository likeRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PostInteractionService interactionService;

    private User user;
    private User postAuthor;
    private BlogPost post;

    @BeforeEach
    void setUp() {
        user = user();
        postAuthor = user();
        post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setAuthor(postAuthor);
        post.setTitle("Test post");
        post.setSlug("test-post");
    }

    @Test
    void likePostIsIdempotent() {
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(blogPostService.requirePublicPost(post.getId())).thenReturn(post);
        when(likeRepository.insertIfAbsent(any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(1);
        when(likeRepository.existsByPostAndUser(post, user)).thenReturn(true);
        when(likeRepository.countByPostId(post.getId())).thenReturn(1L);

        var response = interactionService.likePost(post.getId(), user);

        verify(likeRepository).insertIfAbsent(any(UUID.class), any(UUID.class), any(UUID.class));
        verify(emailService).sendNewLikeNotification(
                postAuthor.getEmail(),
                postAuthor.getName(),
                user.getName(),
                post.getTitle(),
                post.getSlug()
        );
        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());
    }

    @Test
    void likePostDoesNotCreateDuplicateLike() {
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(blogPostService.requirePublicPost(post.getId())).thenReturn(post);
        when(likeRepository.insertIfAbsent(any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(0);
        when(likeRepository.existsByPostAndUser(post, user)).thenReturn(true);
        when(likeRepository.countByPostId(post.getId())).thenReturn(1L);

        interactionService.likePost(post.getId(), user);

        verify(likeRepository).insertIfAbsent(any(UUID.class), any(UUID.class), any(UUID.class));
        verify(emailService, never()).sendNewLikeNotification(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void addCommentNotifiesPostAuthor() {
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(blogPostService.requirePublicPost(post.getId())).thenReturn(post);
        when(commentRepository.save(any(PostComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        interactionService.addComment(post.getId(), new CommentRequest("Helpful article"), user);

        verify(emailService).sendNewCommentNotification(
                postAuthor.getEmail(),
                postAuthor.getName(),
                user.getName(),
                post.getTitle(),
                post.getSlug(),
                "Helpful article"
        );
    }

    @Test
    void updateCommentRejectsAnotherUser() {
        User author = user();
        PostComment comment = new PostComment();
        comment.setId(UUID.randomUUID());
        comment.setAuthor(author);
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(
                ForbiddenException.class,
                () -> interactionService.updateComment(
                        comment.getId(),
                        new CommentRequest("Updated comment"),
                        user
                )
        );
    }

    private User user() {
        User result = new User();
        result.setId(UUID.randomUUID());
        result.setName("Test User");
        result.setEmail(result.getId() + "@example.com");
        result.setRole(Role.USER);
        result.setEmailVerified(true);
        result.setActive(true);
        return result;
    }
}
