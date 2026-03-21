package com.tradingplatform.user;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("registerUser with valid email/password creates user with hashed password")
    void registerUser_validInput_createsUserWithHashedPassword() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String hashedPassword = "$2a$10$hashedpassword";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = userService.registerUser(email, password);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(hashedPassword, result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser with duplicate email throws ApiException")
    void registerUser_duplicateEmail_throwsApiException() {
        // Arrange
        String email = "existing@example.com";
        String password = "password123";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.registerUser(email, password);
        });

        assertEquals(ErrorCode.REGISTRATION_FAILED, exception.getErrorCode());
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser with short password throws ApiException")
    void registerUser_shortPassword_throwsApiException() {
        // Arrange
        String email = "test@example.com";
        String shortPassword = "short";

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            userService.registerUser(email, shortPassword);
        });

        assertEquals(ErrorCode.REGISTRATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("8 characters"));
    }

    @Test
    @DisplayName("loadUserByUsername returns UserPrincipal for existing user")
    void loadUserByUsername_existingUser_returnsUserPrincipal() {
        // Arrange
        String email = "test@example.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .password("hashedPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        UserPrincipal result = (UserPrincipal) userService.loadUserByUsername(email);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(email, result.getEmail());
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException for non-existing user")
    void loadUserByUsername_nonExistingUser_throwsException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });
    }

    @Test
    @DisplayName("updateRefreshTokenHash updates user's refresh token hash")
    void updateRefreshTokenHash_updatesUser() {
        // Arrange
        Long userId = 1L;
        String tokenHash = "hashedRefreshToken";
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.updateRefreshTokenHash(userId, tokenHash);

        // Assert
        verify(userRepository).save(argThat(u -> tokenHash.equals(u.getRefreshTokenHash())));
    }
}