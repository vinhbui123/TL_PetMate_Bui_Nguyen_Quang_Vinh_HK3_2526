package com.petmate.server.repository;

import com.petmate.server.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrganizationId(Long orgId);
    List<OrganizationMember> findByUserId(Long userId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long orgId, Long userId);
    boolean existsByOrganizationIdAndUserId(Long orgId, Long userId);
    long countByOrganizationId(Long orgId);
}
