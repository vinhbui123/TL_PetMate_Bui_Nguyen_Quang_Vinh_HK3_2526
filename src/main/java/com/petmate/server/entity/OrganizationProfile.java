package com.petmate.server.entity;

import com.petmate.server.enums.OrgType;
import com.petmate.server.enums.VerificationLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // owner of the organization profile

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private OrgType orgType = OrgType.PRIVATE_RESCUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_level", nullable = false, columnDefinition = "VARCHAR(10)")
    @Builder.Default
    private VerificationLevel verificationLevel = VerificationLevel.FULL;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    private String contact;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "establishment_number", length = 100)
    private String establishmentNumber;

    @Column(length = 500)
    private String website;

    @Column(length = 500)
    private String fanpage;

    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_phone", length = 20)
    private String representativePhone;

    @Column(name = "representative_email")
    private String representativeEmail;

    @Column(name = "representative_id_type", length = 20)
    private String representativeIdType;

    @Column(name = "representative_id_number", length = 50)
    private String representativeIdNumber;

    @Column(name = "representative_id_front_url", length = 1000)
    private String representativeIdFrontUrl;

    @Column(name = "representative_avatar_url", length = 1000)
    private String representativeAvatarUrl;

    @Column(name = "representative_social_url", length = 1000)
    private String representativeSocialUrl;

    @Column(name = "representative_role", length = 50)
    private String representativeRole;

    @Column(name = "rep_last_verified_at")
    private LocalDateTime repLastVerifiedAt;

    @Column(name = "sterilization_policy")
    @Builder.Default
    private Boolean sterilizationPolicy = false;

    @Column(name = "vaccination_policy")
    @Builder.Default
    private Boolean vaccinationPolicy = false;

    @Column(name = "policy_description", columnDefinition = "TEXT")
    private String policyDescription;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, NEEDS_SUPPLEMENT, APPROVED, REJECTED

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_until")
    private LocalDateTime verifiedUntil;

    @Column(name = "agreed_terms")
    @Builder.Default
    private Boolean agreedTerms = false;

    @Column(name = "agreed_terms_at")
    private LocalDateTime agreedTermsAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public boolean isVerified() {
        return "APPROVED".equals(status)
                && verifiedUntil != null
                && verifiedUntil.isAfter(LocalDateTime.now());
    }

    @Transient
    public String getBadgeLabel() {
        if (!isVerified()) return null;
        return orgType == OrgType.INDEPENDENT_FOSTER
                ? "CÃ¡ nhÃ¢n Ä‘Ã£ xÃ¡c minh"
                : "Tá»• chá»©c Ä‘Ã£ xÃ¡c minh";
    }
}
