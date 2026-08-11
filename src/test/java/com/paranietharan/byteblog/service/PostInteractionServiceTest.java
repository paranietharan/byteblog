package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.CommentRequest;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostComment;
import com.paranietharan.byteblog.entity.PostLike;
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

    @InjectMocks
    private PostInteractionService interactionService;

    private User user;
    private BlogPost post;

    @BeforeEach
    void setUp() {
        user = user();
        post = new BlogPost();
        post.setId(UUID.randomUUID());
    }

    @Test
    void likePostIsIdempotent() {
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(blogPostService.requirePublicPost(post.getId())).thenReturn(post);
        when(likeRepository.existsByPostAndUser(post, user)).thenReturn(false, true);
        when(likeRepository.countByPostId(post.getId())).thenReturn(1L);

        var response = interactionService.likePost(post.getId(), user);

        verify(likeRepository).save(any(PostLike.class));
        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());
    }

    @Test
    void likePostDoesNotCreateDuplicateLike() {
        when(authenticatedUserService.requireVerifiedUser(user)).thenReturn(user);
        when(blogPostService.requirePublicPost(post.getId())).thenReturn(post);
        when(likeRepository.existsByPostAndUser(post, user)).thenReturn(true);
        when(likeRepository.countByPostId(post.getId())).thenReturn(1L);

        interactionService.likePost(post.getId(), user);

        verify(likeRepository, never()).save(any(PostLike.class));
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
        result.setRole(Role.USER);
        result.setEmailVerified(true);
        result.setActive(true);
        return result;
    }
}
