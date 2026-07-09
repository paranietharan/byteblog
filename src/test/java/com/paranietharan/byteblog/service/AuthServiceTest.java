package com.paranietharan.byteblog.service;

import com.paranietharan.byteblog.dto.LoginRequest;
import com.paranietharan.byteblog.dto.RegisterRequest;
import com.paranietharan.byteblog.entity.Role;
import com.paranietharan.byteblog.entity.User;
import com.paranietharan.byteblog.exception.BadRequestException;
import com.paranietharan.byteblog.exception.UnauthorizedException;
import com.paranietharan.byteblog.repository.EmailVerificationTokenRepository;
import com.paranietharan.byteblog.repository.RefreshTokenRepository;
import com.paranietharan.byteblog.repository.UserRepository;
import com.paranietharan.byteblog.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(Role.USER);
        testUser.setActive(true);
        testUser.setEmailVerified(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        registerRequest = new RegisterRequest();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateAccessTokenFromEmail(anyString())).thenReturn("accessToken");
        when(tokenProvider.generateRefreshToken()).thenReturn("refreshToken");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        // Act
        var response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        testUser.setEmailVerified(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("accessToken");
        when(tokenProvider.generateRefreshToken()).thenReturn("refreshToken");
        when(tokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        // Act
        var response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
    }

    @Test
    void testLogin_EmailNotVerified() {
        // Arrange
        testUser.setEmailVerified(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testGetUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        User user = authService.getUserByEmail(testUser.getEmail());

        // Assert
        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
        assertEquals(testUser.getEmail(), user.getEmail());
    }

    @Test
    void testGetUserByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> authService.getUserByEmail("nonexistent@example.com"));
    }
}
