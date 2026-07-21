package com.petmate.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.petmate.server.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    private String password;

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

    @Column(length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

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

    @PrePersist
    @PreUpdate
    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
