package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.ForbiddenException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User requireVerifiedUser(User principal) {
        User user = requireUser(principal);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException("Email verification is required");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireAdmin(User principal) {
        User user = requireVerifiedUser(principal);
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Administrator access is required");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User requireUser(User principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedException("Authentication is required");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UnauthorizedException("User account is inactive");
        }
        return user;
    }
}
