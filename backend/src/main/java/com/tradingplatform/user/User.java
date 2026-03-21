package com.tradingplatform.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    @Email
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "display_name", length = 100)
    @Size(max = 100)
    private String displayName;

    @Column(name = "avatar_path", length = 500)
    private String avatarPath;

    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "is_profile_complete", nullable = false)
    @Builder.Default
    private boolean profileComplete = false;

    /**
     * Returns the display name if set, otherwise returns "New User" placeholder.
     * Implements D-08 from CONTEXT.md.
     */
    @Transient
    public String getDisplayNameOrFallback() {
        return displayName != null ? displayName : "New User";
    }

    /**
     * Updates the profileComplete flag based on whether display name is set.
     * Implements D-05 and D-06 from CONTEXT.md.
     */
    public void updateProfileComplete() {
        this.profileComplete = displayName != null && !displayName.isBlank();
    }
}