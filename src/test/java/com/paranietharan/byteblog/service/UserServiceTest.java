package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.ChangePasswordRequest;
import com.paranietharan.byteblog.dto.ChangeNameRequest;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.ResourceNotFoundException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.EmailVerificationTokenRepository;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Long testUserId = 1L;

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
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        var response = userService.getCurrentUser(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getEmail(), response.getEmail());
    }

    @Test
    void testGetCurrentUser_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser(testUserId));
    }

    @Test
    void testChangePassword_Success() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        var response = userService.changePassword(testUserId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getSuccess());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
    }

    @Test
    void testChangePassword_IncorrectCurrentPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> userService.changePassword(testUserId, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeName_Success() {
        // Arrange
        ChangeNameRequest request = new ChangeNameRequest();
        request.setName("Jane Doe");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        var response = userService.changeName(testUserId, request);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testChangeName_UserNotFound() {
        // Arrange
        ChangeNameRequest request = new ChangeNameRequest();
        request.setName("Jane Doe");

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.changeName(testUserId, request));
    }
}
