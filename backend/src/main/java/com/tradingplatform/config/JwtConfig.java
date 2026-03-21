package com.tradingplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * Base64-encoded secret key for JWT signing.
     * Must be at least 256 bits (32 bytes) for HS256 algorithm.
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Default: 15 minutes (900,000 ms)
     */
    private long accessTokenExpiration = 900000L;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 7 days (604,800,000 ms)
     */
    private long refreshTokenExpiration = 604800000L;
}