package com.petmate.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.petmate.server.enums.AdStatus;
import com.petmate.server.enums.Gender;
import com.petmate.server.enums.ListingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets", indexes = {
    @Index(name = "idx_pets_user_id", columnList = "user_id"),
    @Index(name = "idx_pets_category", columnList = "category"),
    @Index(name = "idx_pets_status", columnList = "status"),
    @Index(name = "idx_pets_category_status", columnList = "category, status"),
    @Index(name = "idx_pets_listing_type", columnList = "listing_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "listing_type", length = 20, nullable = false)
    @Builder.Default
    private ListingType listingType = ListingType.SALE;

    @Column(nullable = false)
    private String name;

    private String breed;

    @Column(name = "age_months")
    private Integer ageMonths;

    private Double weight;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(length = 10)
    private Gender gender;

    @Column(precision = 12, scale = 0)
    private BigDecimal price;

    @Column(name = "is_vaccinated")
    @Builder.Default
    private Boolean isVaccinated = false;

    @Column(name = "is_neutered")
    @Builder.Default
    private Boolean isNeutered = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private String category;

    private String address;
    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"providerId", "provider", "createdAt", "updatedAt"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    @JsonIgnoreProperties({"user", "hibernateLazyInitializer", "handler", "createdAt", "updatedAt"})
    private OrganizationProfile organization;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(length = 20)
    @Builder.Default
    private AdStatus status = AdStatus.AVAILABLE;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
