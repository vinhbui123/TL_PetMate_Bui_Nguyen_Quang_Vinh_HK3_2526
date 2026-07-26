package com.petmate.server.entity;

import com.petmate.server.enums.OrgMemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_members", indexes = {
        @Index(name = "idx_orgmember_org", columnList = "org_id"),
        @Index(name = "idx_orgmember_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_org_user", columnNames = {"org_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private OrganizationProfile organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private OrgMemberRole memberRole = OrgMemberRole.COLLABORATOR;

    @Column(name = "invited_by")
    private Long invitedBy;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
