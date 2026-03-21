package com.tradingplatform.user;

import com.tradingplatform.exception.ApiException;
import com.tradingplatform.exception.ErrorCode;
import com.tradingplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with the given email and password.
     * Implements D-03: Password must be at least 8 characters.
     * Implements D-04: Generic error message for duplicate email.
     */
    @Transactional
    public User registerUser(String email, String password) {
        // Validate password length (D-03)
        if (password == null || password.length() < 8) {
            throw new ApiException(ErrorCode.REGISTRATION_FAILED,
                    "Password must be at least 8 characters");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            // D-04: Generic error to prevent email enumeration
            throw new ApiException(ErrorCode.REGISTRATION_FAILED, "Email already registered");
        }

        // Create user with hashed password
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .profileComplete(false)
                .build();

        return userRepository.save(user);
    }

    /**
     * Loads user by email for Spring Security authentication.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return UserPrincipal.create(user);
    }

    /**
     * Updates the refresh token hash for a user.
     * Used to track valid refresh tokens for token rotation.
     */
    @Transactional
    public void updateRefreshTokenHash(Long userId, String tokenHash) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.setRefreshTokenHash(tokenHash);
        userRepository.save(user);
    }

    /**
     * Clears the refresh token hash for a user (logout).
     */
    @Transactional
    public void clearRefreshTokenHash(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshTokenHash(null);
            userRepository.save(user);
        });
    }

    /**
     * Finds a user by ID.
     */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Finds a user by email.
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}