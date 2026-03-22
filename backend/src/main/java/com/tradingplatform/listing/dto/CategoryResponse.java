package com.tradingplatform.listing.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CategoryResponse {
    Long id;
    String name;
    String slug;
    String description;
    Long parentId;
    Integer displayOrder;
    List<CategoryResponse> children;
}
