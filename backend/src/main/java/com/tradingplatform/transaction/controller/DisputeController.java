package com.tradingplatform.transaction.controller;

import com.tradingplatform.security.UserPrincipal;
import com.tradingplatform.transaction.dto.*;
import com.tradingplatform.transaction.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dispute operations.
 * Per D-15 to D-18: Admin-mediated dispute resolution.
 */
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    /**
     * Open a dispute for a transaction.
     * Per D-16: Either party can raise dispute after DELIVERED state.
     *
     * @param transactionId the transaction ID
     * @param principal the authenticated user
     * @param request the dispute request
     * @return the created dispute response
     */
    @PostMapping("/transactions/{transactionId}")
    public ResponseEntity<DisputeResponse> openDispute(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DisputeRequest request) {
        DisputeResponse response = disputeService.openDispute(
            transactionId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get dispute by transaction ID.
     *
     * @param transactionId the transaction ID
     * @param principal the authenticated user
     * @return the dispute response
     */
    @GetMapping("/transactions/{transactionId}")
    public DisputeResponse getDispute(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return disputeService.getDisputeByTransaction(transactionId, principal.getId());
    }

    /**
     * Resolve a dispute (admin only - for v1).
     * Per D-17: Admin-mediated resolution.
     *
     * @param disputeId the dispute ID
     * @param request the resolution request
     * @return the updated dispute response
     */
    @PostMapping("/{disputeId}/resolve")
    public DisputeResponse resolveDispute(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeResolutionRequest request) {
        return disputeService.resolveDispute(disputeId, request);
    }
}