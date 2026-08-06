package com.petmate.server.service;

import com.petmate.server.dto.*;
import com.petmate.server.entity.*;
import com.petmate.server.enums.*;
import com.petmate.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationProfileRepository orgRepository;
    private final OrganizationDocumentRepository docRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PlatformConfigService configService;
    private final FirebaseService firebaseService;
    private final SystemLogService systemLogService;
    private final CloudinaryService cloudinaryService;
    private final PetRepository petRepository;
    private final AdoptionApplicationRepository adoptionRepo;

    public OrganizationProfileDto createOrganization(Long userId, OrganizationProfileDto dto) {
        if (orgRepository.existsByUserId(userId)) {
            OrganizationProfile existing = orgRepository.findByUserId(userId).get();
            if ("REJECTED".equals(existing.getStatus())) {
                orgRepository.delete(existing); // Delete rejected profile so they can re-apply
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người dùng đã là thành viên trạm cứu hộ");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Leave existing org if they are a member
        List<OrganizationMember> memberships = memberRepository.findByUserId(user.getId());
        if (!memberships.isEmpty()) {
            OrganizationMember member = memberships.get(0);
            long memberCount = memberRepository.countByOrganizationId(member.getOrganization().getId());
            
            if (member.getMemberRole() == com.petmate.server.enums.OrgMemberRole.OWNER && memberCount > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chuyển quyền chủ trạm cho thành viên khác trước khi rời.");
            } else if (member.getMemberRole() == com.petmate.server.enums.OrgMemberRole.OWNER && memberCount == 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn là thành viên duy nhất. Vui lòng giải thể trạm hoặc mời thêm quản lý trước khi rời.");
            }
            
            memberRepository.delete(member);
        }

        OrgType type = OrgType.valueOf(dto.getOrgType() != null ? dto.getOrgType() : "PRIVATE_RESCUE");
        VerificationLevel level = (type == OrgType.INDEPENDENT_FOSTER) ? VerificationLevel.LITE : VerificationLevel.FULL;

        OrganizationProfile org = OrganizationProfile.builder()
                .user(user)
                .orgType(type)
                .verificationLevel(level)
                .name(dto.getName())
                .address(dto.getAddress())
                .contact(dto.getPhone() != null ? dto.getPhone() : dto.getContact())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .foundedYear(dto.getFoundedYear())
                .businessAddress(dto.getBusinessAddress())
                .taxCode(dto.getTaxCode())
                .establishmentNumber(dto.getEstablishmentNumber())
                .website(dto.getWebsite())
                .fanpage(dto.getFanpage())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .representativeName(dto.getRepresentativeName() != null && !dto.getRepresentativeName().isEmpty() ? dto.getRepresentativeName() : user.getFullName())
                .representativePhone(dto.getRepresentativePhone() != null && !dto.getRepresentativePhone().isEmpty() ? dto.getRepresentativePhone() : user.getPhone())
                .representativeEmail(dto.getRepresentativeEmail() != null && !dto.getRepresentativeEmail().isEmpty() ? dto.getRepresentativeEmail() : user.getEmail())
                .representativeIdType(dto.getRepresentativeIdType())
                .representativeIdNumber(dto.getRepresentativeIdNumber())
                .representativeIdFrontUrl(dto.getRepresentativeIdFrontUrl())
                .representativeAvatarUrl(dto.getRepresentativeAvatarUrl())
                .representativeSocialUrl(dto.getRepresentativeSocialUrl())
                .representativeRole(dto.getRepresentativeRole() != null && !dto.getRepresentativeRole().isEmpty() ? dto.getRepresentativeRole() : "OWNER")
                .sterilizationPolicy(dto.getSterilizationPolicy() != null ? dto.getSterilizationPolicy() : false)
                .vaccinationPolicy(dto.getVaccinationPolicy() != null ? dto.getVaccinationPolicy() : false)
                .policyDescription(dto.getPolicyDescription())
                .agreedTerms(dto.getAgreedTerms() != null ? dto.getAgreedTerms() : false)
                .agreedTermsAt(Boolean.TRUE.equals(dto.getAgreedTerms()) ? LocalDateTime.now() : null)
                .status(OrgStatus.PENDING.name())
                .build();

        org = orgRepository.save(org);
        
        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(org)
                .user(user)
                .memberRole(OrgMemberRole.OWNER)
                .build();
        memberRepository.save(ownerMember);
        
        systemLogService.info("ORGANIZATION_CREATED", "USER_" + userId, "Created organization profile: " + org.getId());
        return toDto(org);
    }

    public OrganizationProfileDto updateOrganization(Long id, Long userId, OrganizationProfileDto dto) {
        OrganizationProfile org = orgRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));

        if (!org.getUser().getId().equals(userId) && !hasManagerRole(id, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật tổ chức này");
        }

        if (!"PENDING".equals(org.getStatus()) && !"NEEDS_SUPPLEMENT".equals(org.getStatus()) && !"APPROVED".equals(org.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể cập nhật hồ sơ tổ chức ở trạng thái: " + org.getStatus());
        }

        if (dto.getName() != null) org.setName(dto.getName());
        if (dto.getAddress() != null) org.setAddress(dto.getAddress());
        if (dto.getDescription() != null) org.setDescription(dto.getDescription());
        if (dto.getLogoUrl() != null) org.setLogoUrl(dto.getLogoUrl());
        
        if (dto.getFoundedYear() != null) org.setFoundedYear(dto.getFoundedYear());
        if (dto.getBusinessAddress() != null) org.setBusinessAddress(dto.getBusinessAddress());
        if (dto.getTaxCode() != null) org.setTaxCode(dto.getTaxCode());
        if (dto.getEstablishmentNumber() != null) org.setEstablishmentNumber(dto.getEstablishmentNumber());
        if (dto.getWebsite() != null) org.setWebsite(dto.getWebsite());
        if (dto.getFanpage() != null) org.setFanpage(dto.getFanpage());
        if (dto.getEmail() != null) org.setEmail(dto.getEmail());
        if (dto.getPhone() != null) {
            org.setPhone(dto.getPhone());
            org.setContact(dto.getPhone());
        }
        
        if (dto.getRepresentativeName() != null) org.setRepresentativeName(dto.getRepresentativeName());
        if (dto.getRepresentativePhone() != null) org.setRepresentativePhone(dto.getRepresentativePhone());
        if (dto.getRepresentativeEmail() != null) org.setRepresentativeEmail(dto.getRepresentativeEmail());
        if (dto.getRepresentativeIdType() != null) org.setRepresentativeIdType(dto.getRepresentativeIdType());
        if (dto.getRepresentativeIdNumber() != null) org.setRepresentativeIdNumber(dto.getRepresentativeIdNumber());
        if (dto.getRepresentativeIdFrontUrl() != null) org.setRepresentativeIdFrontUrl(dto.getRepresentativeIdFrontUrl());
        if (dto.getRepresentativeAvatarUrl() != null) org.setRepresentativeAvatarUrl(dto.getRepresentativeAvatarUrl());
        if (dto.getRepresentativeSocialUrl() != null) org.setRepresentativeSocialUrl(dto.getRepresentativeSocialUrl());
        if (dto.getRepresentativeRole() != null) org.setRepresentativeRole(dto.getRepresentativeRole());

        if (dto.getSterilizationPolicy() != null) org.setSterilizationPolicy(dto.getSterilizationPolicy());
        if (dto.getVaccinationPolicy() != null) org.setVaccinationPolicy(dto.getVaccinationPolicy());
        if (dto.getPolicyDescription() != null) org.setPolicyDescription(dto.getPolicyDescription());

        if ("NEEDS_SUPPLEMENT".equals(org.getStatus())) {
            org.setStatus("PENDING");
        }

        org = orgRepository.save(org);
        return toDto(org);
    }

    public OrganizationProfileDto getOrganization(Long id) {
        OrganizationProfile org = orgRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
        return toDto(org);
    }

    public OrganizationProfileDto getMyOrganization(Long userId) {
        OrganizationProfile org = orgRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Check if they are a member
                    return memberRepository.findByUserId(userId).stream().findFirst()
                            .map(OrganizationMember::getOrganization)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You don't belong to any organization"));
                });
        return toDto(org);
    }

    public List<OrganizationProfileDto> listByStatus(String status) {
        return orgRepository.findByStatus(status).stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrganizationProfileDto reviewOrganization(Long id, OrgReviewRequestDto reviewDto) {
        OrganizationProfile org = orgRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
        
        OrgStatus status = OrgStatus.valueOf(reviewDto.getStatus());
        org.setStatus(status.name());

        if (status == OrgStatus.APPROVED) {
            if (org.getRepresentativeName() == null || org.getRepresentativeName().isEmpty() ||
                org.getRepresentativePhone() == null || org.getRepresentativePhone().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot approve organization without primary representative details");
            }
            org.setStatus(OrgStatus.APPROVED.name());
            int verifiedDays = configService.getIntValue("org.verified_duration_days", 365);
            org.setVerifiedAt(LocalDateTime.now());
            org.setVerifiedUntil(LocalDateTime.now().plusDays(verifiedDays));
            
            User owner = org.getUser();
            if (owner.getRole() == com.petmate.server.enums.RoleType.MEMBER || owner.getRole() == com.petmate.server.enums.RoleType.PENDING_RESCUE) {
                owner.setRole(com.petmate.server.enums.RoleType.RESCUE_ORG);
                userRepository.save(owner);
            }
            
            // Add owner as a member if not exists
            if (!memberRepository.existsByOrganizationIdAndUserId(org.getId(), owner.getId())) {
                OrganizationMember member = OrganizationMember.builder()
                        .organization(org)
                        .user(owner)
                        .memberRole(OrgMemberRole.OWNER)
                        .build();
                memberRepository.save(member);
            }
            
            sendNotification(owner.getId(), "Hồ sơ đã được duyệt!", "Tổ chức của bạn đã được xác minh thành công.");
        } else if (status == OrgStatus.REJECTED) {
            org.setRejectionReason(reviewDto.getRejectionReason());
            User owner = org.getUser();
            if (owner.getRole() == com.petmate.server.enums.RoleType.PENDING_RESCUE) {
                owner.setRole(com.petmate.server.enums.RoleType.MEMBER);
                userRepository.save(owner);
            }
            sendNotification(owner.getId(), "Hồ sơ bị từ chối", "Lý do: " + reviewDto.getRejectionReason());
        } else if (status == OrgStatus.NEEDS_SUPPLEMENT) {
            org.setAdminNote(reviewDto.getAdminNote());
            sendNotification(org.getUser().getId(), "Hồ sơ cần bổ sung", "Ghi chú: " + reviewDto.getAdminNote());
        }

        org = orgRepository.save(org);
        systemLogService.info("ORGANIZATION_REVIEWED", "ADMIN", "Organization " + id + " set to " + status.name());
        return toDto(org);
    }

    public OrgDocumentDto uploadDocument(Long orgId, Long userId, String docType, MultipartFile file) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
                
        if (!org.getUser().getId().equals(userId) && !hasManagerRole(orgId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        
        try {
            String url = cloudinaryService.uploadDocument(file);
            OrganizationDocument doc = OrganizationDocument.builder()
                    .organization(org)
                    .docType(OrgDocType.valueOf(docType))
                    .fileUrl(url)
                    .fileName(file.getOriginalFilename())
                    .build();
            doc = docRepository.save(doc);
            
            OrgDocumentDto docDto = new OrgDocumentDto();
            docDto.setId(doc.getId());
            docDto.setDocType(doc.getDocType().name());
            docDto.setFileUrl(doc.getFileUrl());
            docDto.setFileName(doc.getFileName());
            return docDto;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload document");
        }
    }

    public OrganizationProfileDto uploadLogo(Long orgId, Long userId, MultipartFile file) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
                
        if (!org.getUser().getId().equals(userId) && !hasManagerRole(orgId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        
        try {
            String url = cloudinaryService.uploadImage(file);
            org.setLogoUrl(url);
            org = orgRepository.save(org);
            return toDto(org);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload logo");
        }
    }

    public void deleteDocument(Long orgId, Long docId, Long userId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức");
        }
        if (!orgRepository.findById(orgId).get().getUser().getId().equals(userId) && !hasManagerRole(orgId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        docRepository.deleteById(docId);
    }

    public OrgMemberDto inviteMember(Long orgId, Long inviterUserId, InviteMemberDto inviteDto) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
                
        if (org.getOrgType() == OrgType.INDEPENDENT_FOSTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Independent foster cannot have additional members");
        }

        if (!org.getUser().getId().equals(inviterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can invite members");
        }
        
        User invitee = userRepository.findByEmail(inviteDto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with email not found"));
                
        if (memberRepository.existsByOrganizationIdAndUserId(orgId, invitee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người dùng đã là thành viên của trạm");
        }
        
        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(invitee)
                .memberRole(OrgMemberRole.valueOf(inviteDto.getMemberRole()))
                .status(com.petmate.server.enums.OrgMemberStatus.PENDING)
                .invitedBy(inviterUserId)
                .build();
                
        member = memberRepository.save(member);
        
        try {
            firebaseService.sendNotification(
                invitee.getId(), 
                "Lời mời tham gia Trạm cứu hộ", 
                "Bạn nhận được lời mời tham gia " + org.getName(), 
                java.util.Map.of("type", "org_invite", "orgId", orgId.toString(), "memberId", member.getId().toString())
            );
        } catch (Exception e) {}
        
        return toMemberDto(member);
    }

    public OrgMemberDto acceptInvitation(Long orgId, Long memberId, Long userId) {
        OrganizationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên"));
        
        if (!member.getOrganization().getId().equals(orgId) || !member.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này");
        }
        
        member.setStatus(com.petmate.server.enums.OrgMemberStatus.ACTIVE);
        member = memberRepository.save(member);
        return toMemberDto(member);
    }

    public void rejectInvitation(Long orgId, Long memberId, Long userId) {
        OrganizationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên"));
        
        if (!member.getOrganization().getId().equals(orgId) || !member.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này");
        }
        
        memberRepository.delete(member);
    }

    public void removeMember(Long orgId, Long ownerUserId, Long memberId) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
                
        if (!org.getUser().getId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can remove members");
        }
        
        OrganizationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
                
        if (member.getUser().getId().equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa chủ sở hữu trạm");
        }
        try {
            firebaseService.sendNotification(
                member.getUser().getId(),
                "Thông báo từ Trạm cứu hộ",
                "Bạn đã bị xóa khỏi tổ chức " + org.getName(),
                null
            );
        } catch (Exception e) {}
        
        memberRepository.delete(member);
    }

    public List<OrgMemberDto> getMembers(Long orgId) {
        return memberRepository.findByOrganizationId(orgId).stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }
    
    private boolean hasManagerRole(Long orgId, Long userId) {
        return memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .map(m -> m.getMemberRole() == OrgMemberRole.OWNER || m.getMemberRole() == OrgMemberRole.MANAGER)
                .orElse(false);
    }

    private OrganizationProfileDto toDto(OrganizationProfile org) {
        OrganizationProfileDto dto = new OrganizationProfileDto();
        dto.setId(org.getId());
        dto.setUserId(org.getUser().getId());
        dto.setName(org.getName());
        dto.setAddress(org.getAddress());
        dto.setContact(org.getContact());
        dto.setDescription(org.getDescription());
        dto.setLogoUrl(org.getLogoUrl());
        dto.setStatus(org.getStatus());
        
        dto.setOrgType(org.getOrgType().name());
        dto.setVerificationLevel(org.getVerificationLevel().name());
        dto.setFoundedYear(org.getFoundedYear());
        dto.setBusinessAddress(org.getBusinessAddress());
        dto.setTaxCode(org.getTaxCode());
        dto.setEstablishmentNumber(org.getEstablishmentNumber());
        dto.setWebsite(org.getWebsite());
        dto.setFanpage(org.getFanpage());
        dto.setEmail(org.getEmail());
        dto.setPhone(org.getPhone());
        dto.setRepresentativeName(org.getRepresentativeName());
        dto.setRepresentativePhone(org.getRepresentativePhone());
        dto.setRepresentativeEmail(org.getRepresentativeEmail());
        dto.setRepresentativeIdType(org.getRepresentativeIdType());
        dto.setRepresentativeIdNumber(org.getRepresentativeIdNumber());
        dto.setRepresentativeIdFrontUrl(org.getRepresentativeIdFrontUrl());
        dto.setRepresentativeAvatarUrl(org.getRepresentativeAvatarUrl());
        dto.setRepresentativeSocialUrl(org.getRepresentativeSocialUrl());
        dto.setRepresentativeRole(org.getRepresentativeRole());
        dto.setRepLastVerifiedAt(org.getRepLastVerifiedAt() != null ? org.getRepLastVerifiedAt().toString() : null);
        dto.setSterilizationPolicy(org.getSterilizationPolicy());
        dto.setVaccinationPolicy(org.getVaccinationPolicy());
        dto.setPolicyDescription(org.getPolicyDescription());
        dto.setAgreedTerms(org.getAgreedTerms());
        
        dto.setAdminNote(org.getAdminNote());
        dto.setRejectionReason(org.getRejectionReason());
        dto.setVerifiedAt(org.getVerifiedAt() != null ? org.getVerifiedAt().toString() : null);
        dto.setVerifiedUntil(org.getVerifiedUntil() != null ? org.getVerifiedUntil().toString() : null);
        dto.setIsVerified(org.isVerified());
        dto.setBadgeLabel(org.getBadgeLabel());
        
        dto.setOwnerName(org.getUser().getFullName());
        
        List<OrgDocumentDto> docs = docRepository.findByOrganizationId(org.getId()).stream().map(d -> {
            OrgDocumentDto dDto = new OrgDocumentDto();
            dDto.setId(d.getId());
            dDto.setDocType(d.getDocType().name());
            dDto.setFileUrl(d.getFileUrl());
            dDto.setFileName(d.getFileName());
            return dDto;
        }).collect(Collectors.toList());
        dto.setDocuments(docs);
        
        List<OrgMemberDto> members = memberRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toMemberDto).collect(Collectors.toList());
        dto.setMembers(members);
        
        return dto;
    }
    
    private OrgMemberDto toMemberDto(OrganizationMember member) {
        OrgMemberDto dto = new OrgMemberDto();
        dto.setId(member.getId());
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getFullName());
        dto.setUserEmail(member.getUser().getEmail());
        dto.setUserAvatarUrl(member.getUser().getAvatarUrl());
        dto.setMemberRole(member.getMemberRole().name());
        dto.setStatus(member.getStatus().name());
        return dto;
    }
    
    private void sendNotification(Long userId, String title, String body) {
        try {
            firebaseService.sendNotification(userId, title, body, null);
        } catch (Exception e) {
            // swallow to avoid breaking flow
        }
    }

    public void leaveOrganization(Long orgId, Long userId) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
        
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không phải là thành viên của tổ chức này"));

        long memberCount = memberRepository.countByOrganizationId(orgId);

        if (member.getMemberRole() == OrgMemberRole.OWNER && memberCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chuyển quyền chủ trạm cho thành viên khác trước khi rời.");
        } else if (member.getMemberRole() == OrgMemberRole.OWNER && memberCount == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn là thành viên duy nhất. Vui lòng giải thể trạm trước khi rời.");
        }

        // Xóa thành viên
        memberRepository.delete(member);

        // Giáng cấp role nếu họ là RESCUE_ORG
        User user = userRepository.findById(userId).get();
        if (user.getRole() == RoleType.RESCUE_ORG) {
            user.setRole(RoleType.MEMBER);
            userRepository.save(user);
        }
    }

    public void dissolveOrganization(Long orgId, Long userId) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
        
        if (!org.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ sở hữu mới có quyền giải thể trạm");
        }

        long memberCount = memberRepository.countByOrganizationId(orgId);
        if (memberCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng tháo tất cả các thành viên khác trước khi giải thể trạm");
        }

        // Xóa thành viên cuối cùng
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElse(null);
        if (member != null) {
            memberRepository.delete(member);
        }

        // Xóa tổ chức
        orgRepository.delete(org);

        // Giáng cấp role
        User user = userRepository.findById(userId).get();
        if (user.getRole() == RoleType.RESCUE_ORG) {
            user.setRole(RoleType.MEMBER);
            userRepository.save(user);
        }
    }

    public void transferOwnershipAndLeave(Long orgId, Long currentOwnerId, Long newOwnerId) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));
        
        if (!org.getUser().getId().equals(currentOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ sở hữu mới có quyền chuyển nhượng");
        }

        OrganizationMember newOwnerMember = memberRepository.findByOrganizationIdAndUserId(orgId, newOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người nhận chuyển nhượng không phải là thành viên của trạm"));

        OrganizationMember currentOwnerMember = memberRepository.findByOrganizationIdAndUserId(orgId, currentOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lỗi xác thực thành viên"));

        // Cập nhật người sở hữu mới trong OrganizationProfile
        User newOwnerUser = userRepository.findById(newOwnerId).get();
        org.setUser(newOwnerUser);
        orgRepository.save(org);

        // Đổi role cho người mới
        newOwnerMember.setMemberRole(OrgMemberRole.OWNER);
        memberRepository.save(newOwnerMember);
        if (newOwnerUser.getRole() != RoleType.RESCUE_ORG) {
            newOwnerUser.setRole(RoleType.RESCUE_ORG);
            userRepository.save(newOwnerUser);
        }

        // Xóa owner cũ và giáng cấp
        memberRepository.delete(currentOwnerMember);
        User currentOwnerUser = userRepository.findById(currentOwnerId).get();
        if (currentOwnerUser.getRole() == RoleType.RESCUE_ORG) {
            currentOwnerUser.setRole(RoleType.MEMBER);
            userRepository.save(currentOwnerUser);
        }
    }

    public OrgStatsDto getOrgStats(Long orgId, Jwt jwt) {
        OrganizationProfile org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tổ chức"));

        String uid = jwt.getSubject();
        User currentUser = userRepository.findByProviderId(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        if (!org.getUser().getId().equals(currentUser.getId()) && !hasManagerRole(orgId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view stats");
        }

        long totalPets = petRepository.countByOrganization_Id(orgId);
        long adoptedPets = petRepository.countByOrganization_IdAndStatus(orgId, AdStatus.SOLD);
        long pendingApps = adoptionRepo.countByPet_Organization_IdAndStatus(orgId, AdoptionStatus.PENDING);
        long totalMembers = memberRepository.countByOrganizationId(orgId);

        return OrgStatsDto.builder()
                .totalPets(totalPets)
                .adoptedPets(adoptedPets)
                .pendingAdoptionApps(pendingApps)
                .totalMembers(totalMembers)
                .build();
    }
}
