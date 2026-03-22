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
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found"),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Conversation not found"),
    NOT_CONVERSATION_PARTICIPANT(HttpStatus.FORBIDDEN, "Not a participant in this conversation"),
    CANNOT_CHAT_WITH_SELF(HttpStatus.BAD_REQUEST, "Cannot start a conversation with yourself"),

    // Transaction error codes
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Transaction not found"),
    TRANSACTION_NOT_ELIGIBLE_FOR_RATING(HttpStatus.BAD_REQUEST, "Transaction is not eligible for rating"),
    RATING_WINDOW_EXPIRED(HttpStatus.BAD_REQUEST, "The rating window has expired"),
    ALREADY_RATED(HttpStatus.CONFLICT, "You have already rated this transaction"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid status transition for this transaction"),
    LISTING_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "Listing is not available for transaction"),
    NOT_TRANSACTION_PARTICIPANT(HttpStatus.FORBIDDEN, "You are not a participant in this transaction"),
    DISPUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Dispute not found"),
    DISPUTE_ALREADY_RESOLVED(HttpStatus.BAD_REQUEST, "Dispute has already been resolved");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}