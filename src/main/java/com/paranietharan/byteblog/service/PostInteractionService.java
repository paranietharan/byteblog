package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.AuthorResponse;
import com.paranietharan.byteblog.dto.CommentRequest;
import com.paranietharan.byteblog.dto.CommentResponse;
import com.paranietharan.byteblog.dto.LikeResponse;
import com.paranietharan.byteblog.dto.PageResponse;
import com.paranietharan.byteblog.entity.BlogPost;
import com.paranietharan.byteblog.entity.PostComment;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.repository.PostCommentRepository;
import com.paranietharan.byteblog.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PostInteractionService {

    private final BlogPostService blogPostService;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(UUID postId, int page, int size) {
        blogPostService.requirePublicPost(postId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<CommentResponse> comments = commentRepository
                .findByPostIdAndHiddenFalseOrderByCreatedAtAsc(postId, pageable)
                .map(this::toCommentResponse);
        return PageResponse.from(comments);
    }

    public CommentResponse addComment(UUID postId, CommentRequest request, User principal) {
        User author = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = blogPostService.requirePublicPost(postId);

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.getContent().trim());
        comment.setHidden(false);
        PostComment savedComment = commentRepository.save(comment);
        if (shouldNotifyAuthor(post, author)) {
            emailService.sendNewCommentNotification(
                    post.getAuthor().getEmail(),
                    post.getAuthor().getName(),
                    author.getName(),
                    post.getTitle(),
                    post.getSlug(),
                    savedComment.getContent()
            );
        }
        return toCommentResponse(savedComment);
    }

    public CommentResponse updateComment(UUID commentId, CommentRequest request, User principal) {
        User actor = authenticatedUserService.requireVerifiedUser(principal);
        PostComment comment = findComment(commentId);
        requireCommentOwner(comment, actor);
        comment.setContent(request.getContent().trim());
        return toCommentResponse(commentRepository.save(comment));
    }

    public void deleteOwnComment(UUID commentId, User principal) {
        User actor = authenticatedUserService.requireVerifiedUser(principal);
        PostComment comment = findComment(commentId);
        requireCommentOwner(comment, actor);
        commentRepository.delete(comment);
    }

    public LikeResponse likePost(UUID postId, User principal) {
        User user = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = blogPostService.requirePublicPost(postId);
        boolean created = likeRepository.insertIfAbsent(UUID.randomUUID(), post.getId(), user.getId()) == 1;
        if (created && shouldNotifyAuthor(post, user)) {
            emailService.sendNewLikeNotification(
                    post.getAuthor().getEmail(),
                    post.getAuthor().getName(),
                    user.getName(),
                    post.getTitle(),
                    post.getSlug()
            );
        }
        return buildLikeResponse(post, user);
    }

    public LikeResponse unlikePost(UUID postId, User principal) {
        User user = authenticatedUserService.requireVerifiedUser(principal);
        BlogPost post = blogPostService.requirePublicPost(postId);
        likeRepository.deleteByPostAndUser(post, user);
        return buildLikeResponse(post, user);
    }

    @Transactional(readOnly = true)
    public PostComment findComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    public CommentResponse toCommentResponse(PostComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .author(AuthorResponse.builder()
                        .id(comment.getAuthor().getId())
                        .name(comment.getAuthor().getName())
                        .build())
                .content(comment.getContent())
                .hidden(comment.getHidden())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private LikeResponse buildLikeResponse(BlogPost post, User user) {
        return LikeResponse.builder()
                .postId(post.getId())
                .liked(likeRepository.existsByPostAndUser(post, user))
                .likeCount(likeRepository.countByPostId(post.getId()))
                .build();
    }

    private void requireCommentOwner(PostComment comment, User actor) {
        if (!comment.getAuthor().getId().equals(actor.getId()) && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only manage your own comments");
        }
    }

    private boolean shouldNotifyAuthor(BlogPost post, User actor) {
        return post.getAuthor() != null
                && post.getAuthor().getId() != null
                && !post.getAuthor().getId().equals(actor.getId());
    }
}
