package com.petmate.server.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.petmate.server.enums.RoleType;
import com.petmate.server.enums.UserStatus;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_provider_id", columnList = "provider_id"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;


    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String provider; // local, google, facebook

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private RoleType role = RoleType.MEMBER;

    private String phone;
    
    private String address;
    
    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    private Double latitude;
    private Double longitude;
    
    @Column(name = "cccd", length = 20)
    private String cccd;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "trust_score")
    private Double trustScore;

    @Column(name = "violation_count")
    @Builder.Default
    private Integer violationCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "tokens_valid_after")
    private Instant tokensValidAfter;

    @PrePersist
    @PreUpdate
    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }
    
    @Transient
    public boolean isIdentityVerified() {
        return cccd != null && !cccd.trim().isEmpty();
    }
}
