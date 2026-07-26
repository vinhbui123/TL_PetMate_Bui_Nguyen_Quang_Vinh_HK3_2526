package com.petmate.server.entity;

import com.petmate.server.enums.OrgDocType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_documents", indexes = {
        @Index(name = "idx_orgdoc_org_id", columnList = "org_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private OrganizationProfile organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private OrgDocType docType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
