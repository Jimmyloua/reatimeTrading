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
    PROFILE_INCOMPLETE(HttpStatus.FORBIDDEN, "Profile setup required");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}