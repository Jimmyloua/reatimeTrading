package com.tradingplatform.transaction.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CanRateResponse {
    private Boolean canRate;
}