package com.tradingplatform.content.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "homepage_module_items", indexes = {
        @Index(name = "idx_homepage_module_items_module_order", columnList = "homepage_module_id,display_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomepageModuleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homepage_module_id", nullable = false)
    private HomepageModule homepageModule;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 160)
    private String headline;

    @Column(length = 255)
    private String subheadline;

    @Column(name = "link_type", nullable = false, length = 32)
    private String linkType;

    @Column(name = "link_value", nullable = false, length = 255)
    private String linkValue;

    @Column(name = "accent_label", length = 80)
    private String accentLabel;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
