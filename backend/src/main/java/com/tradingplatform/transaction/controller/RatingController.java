package com.tradingplatform.transaction.controller;

import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.transaction.dto.*;
import com.tradingplatform.transaction.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * Submit a rating for a transaction.
     * Per TRAN-04, TRAN-05, RATE-01.
     */
    @PostMapping("/transactions/{transactionId}")
    public ResponseEntity<RatingResponse> submitRating(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RatingRequest request) {
        RatingResponse response = ratingService.submitRating(
            transactionId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get ratings for a user (visible only).
     * Per RATE-02.
     */
    @GetMapping("/users/{userId}")
    public Page<RatingResponse> getUserRatings(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ratingService.getUserRatings(userId, pageable);
    }

    /**
     * Get recent ratings for user profile (last 5).
     * Per D-40.
     */
    @GetMapping("/users/{userId}/recent")
    public List<RatingResponse> getRecentRatings(@PathVariable Long userId) {
        return ratingService.getRecentRatings(userId);
    }

    /**
     * Get rating summary for user profile.
     * Per RATE-03, RATE-04.
     */
    @GetMapping("/users/{userId}/summary")
    public UserRatingSummary getRatingSummary(@PathVariable Long userId) {
        return ratingService.getRatingSummary(userId);
    }

    /**
     * Check if user can rate a transaction.
     */
    @GetMapping("/transactions/{transactionId}/can-rate")
    public CanRateResponse canRate(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return CanRateResponse.builder()
            .canRate(ratingService.canRate(transactionId, principal.getId()))
            .build();
    }
}