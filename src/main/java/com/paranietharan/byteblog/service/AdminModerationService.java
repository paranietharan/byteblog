package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.MessageResponse;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostComment;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.repository.BlogPostRepository;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminModerationService {

    private final BlogPostService blogPostService;
    private final PostInteractionService interactionService;
    private final BlogPostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailService emailService;

    public MessageResponse hidePost(UUID postId, User principal) {
        User admin = authenticatedUserService.requireAdmin(principal);
        BlogPost post = blogPostService.findPost(postId);
        post.setHidden(true);
        post.setHiddenAt(LocalDateTime.now());
        post.setHiddenBy(admin);
        postRepository.save(post);
        notifyPostAuthor(post, "hidden");
        return new MessageResponse("Post hidden successfully", true);
    }

    public MessageResponse unhidePost(UUID postId, User principal) {
        authenticatedUserService.requireAdmin(principal);
        BlogPost post = blogPostService.findPost(postId);
        post.setHidden(false);
        post.setHiddenAt(null);
        post.setHiddenBy(null);
        postRepository.save(post);
        notifyPostAuthor(post, "restored");
        return new MessageResponse("Post restored successfully", true);
    }

    public MessageResponse deletePost(UUID postId, User principal) {
        authenticatedUserService.requireAdmin(principal);
        BlogPost post = blogPostService.findPost(postId);
        postRepository.delete(post);
        notifyPostAuthor(post, "deleted");
        return new MessageResponse("Post deleted successfully", true);
    }

    public MessageResponse hideComment(UUID commentId, User principal) {
        User admin = authenticatedUserService.requireAdmin(principal);
        PostComment comment = interactionService.findComment(commentId);
        comment.setHidden(true);
        comment.setHiddenAt(LocalDateTime.now());
        comment.setHiddenBy(admin);
        commentRepository.save(comment);
        notifyCommentAuthor(comment, "hidden");
        return new MessageResponse("Comment hidden successfully", true);
    }

    public MessageResponse unhideComment(UUID commentId, User principal) {
        authenticatedUserService.requireAdmin(principal);
        PostComment comment = interactionService.findComment(commentId);
        comment.setHidden(false);
        comment.setHiddenAt(null);
        comment.setHiddenBy(null);
        commentRepository.save(comment);
        notifyCommentAuthor(comment, "restored");
        return new MessageResponse("Comment restored successfully", true);
    }

    public MessageResponse deleteComment(UUID commentId, User principal) {
        authenticatedUserService.requireAdmin(principal);
        PostComment comment = interactionService.findComment(commentId);
        commentRepository.delete(comment);
        notifyCommentAuthor(comment, "deleted");
        return new MessageResponse("Comment deleted successfully", true);
    }

    private void notifyPostAuthor(BlogPost post, String action) {
        User author = post.getAuthor();
        if (author == null) {
            return;
        }
        emailService.sendPostModerationNotification(
                author.getEmail(),
                author.getName(),
                post.getTitle(),
                post.getSlug(),
                action
        );
    }

    private void notifyCommentAuthor(PostComment comment, String action) {
        User author = comment.getAuthor();
        BlogPost post = comment.getPost();
        if (author == null || post == null) {
            return;
        }
        emailService.sendCommentModerationNotification(
                author.getEmail(),
                author.getName(),
                post.getTitle(),
                post.getSlug(),
                action
        );
    }
}
