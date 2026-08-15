package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.ChangePasswordRequest;
import com.paranietharan.byteblog.dto.ChangeNameRequest;
import com.paranietharan.byteblog.dto.ChangeEmailRequest;
import com.paranietharan.byteblog.entity.EmailVerificationToken;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.EmailVerificationTokenRepository;
import com.paranietharan.byteblog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailTokenRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(Role.USER);
        testUser.setActive(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetCurrentUser_Success() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        var response = userService.getCurrentUser(testUserId);

        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getEmail(), response.getEmail());
    }

    @Test
    void testGetCurrentUser_NotFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser(testUserId));
    }

    @Test
    void testChangePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var response = userService.changePassword(testUserId, request);

        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenService).revokeAll(testUser);
    }

    @Test
    void testChangePassword_IncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(testUserId, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeName_Success() {
        ChangeNameRequest request = new ChangeNameRequest();
        request.setName("Jane Doe");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        var response = userService.changeName(testUserId, request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testChangeName_UserNotFound() {
        ChangeNameRequest request = new ChangeNameRequest();
        request.setName("Jane Doe");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.changeName(testUserId, request));
    }

    @Test
    void requestEmailChangeStoresRequestedAddress() {
        ChangeEmailRequest request = new ChangeEmailRequest();
        request.setNewEmail("NEW@example.com");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        userService.requestEmailChange(testUserId, request);

        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(emailTokenRepository).save(tokenCaptor.capture());
        assertEquals("new@example.com", tokenCaptor.getValue().getPendingEmail());
        verify(emailService).sendEmailChangeVerification(
                eq("new@example.com"),
                eq(testUser.getName()),
                anyString()
        );
    }

    @Test
    void verifyEmailChangeAppliesRequestedAddressAndRevokesRefreshTokens() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("email-change-token");
        token.setUser(testUser);
        token.setPendingEmail("new@example.com");
        token.setTokenType("EMAIL_CHANGE");
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        when(emailTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.verifyEmailChange(token.getToken());

        assertEquals("new@example.com", testUser.getEmail());
        assertTrue(token.getUsed());
        verify(refreshTokenService).revokeAll(testUser);
        verify(emailService).sendEmailChangedNotification(
                "john@example.com",
                "new@example.com",
                testUser.getName()
        );
    }
}
