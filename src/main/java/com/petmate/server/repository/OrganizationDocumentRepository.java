package com.petmate.server.repository;

import com.petmate.server.entity.OrganizationDocument;
import com.petmate.server.enums.OrgDocType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationDocumentRepository extends JpaRepository<OrganizationDocument, Long> {
    List<OrganizationDocument> findByOrganizationId(Long orgId);
    void deleteByOrganizationIdAndDocType(Long orgId, OrgDocType docType);
}
