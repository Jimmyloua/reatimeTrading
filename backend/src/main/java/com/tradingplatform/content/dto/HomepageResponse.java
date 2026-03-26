package com.tradingplatform.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomepageResponse {

    private List<HomepageModuleResponse> modules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomepageModuleResponse {
        private String slug;
        private String moduleType;
        private String title;
        private String subtitle;
        private Integer displayOrder;
        private List<HomepageModuleItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomepageModuleItemResponse {
        private String imageUrl;
        private String headline;
        private String subheadline;
        private String linkType;
        private String linkValue;
        private String accentLabel;
        private Integer displayOrder;
    }
}
