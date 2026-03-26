package com.tradingplatform.content.dto;

import com.tradingplatform.listing.dto.ListingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuratedCollectionResponse {

    private String slug;
    private String title;
    private String subtitle;
    private String description;
    private String coverImageUrl;
    private String targetType;
    private String targetValue;
    private Integer displayOrder;
    private List<ListingResponse> items;
}
