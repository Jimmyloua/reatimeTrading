package com.tradingplatform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    REGISTRATION_FAILED(HttpStatus.BAD_REQUEST, "Registration failed"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    PROFILE_INCOMPLETE(HttpStatus.FORBIDDEN, "Profile setup required"),
    INVALID_AVATAR(HttpStatus.BAD_REQUEST, "Invalid avatar file"),
    AVATAR_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar"),
    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Listing not found"),
    LISTING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "You can only modify your own listings"),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Maximum 10 images allowed per listing"),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "Invalid image file"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}