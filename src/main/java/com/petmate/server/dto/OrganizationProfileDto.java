package com.petmate.server.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrganizationProfileDto {
    private Long id;
    private Long userId;
    private String name;
    private String address;
    private String contact;
    private String description;
    private String logoUrl;
    private String status;

    private String orgType;
    private String verificationLevel;

    private Integer foundedYear;
    private String businessAddress;
    private String taxCode;
    private String establishmentNumber;
    private String website;
    private String fanpage;
    private String email;
    private String phone;

    private String representativeName;
    private String representativePhone;
    private String representativeEmail;
    private String representativeIdType;
    private String representativeIdNumber;
    private String representativeIdFrontUrl;
    private String representativeAvatarUrl;
    private String representativeSocialUrl;
    private String representativeRole;
    private String repLastVerifiedAt;

    private List<OrgDocumentDto> documents;

    private Boolean sterilizationPolicy;
    private Boolean vaccinationPolicy;
    private String policyDescription;
    private Boolean agreedTerms;

    private String adminNote;
    private String rejectionReason;
    private String verifiedAt;
    private String verifiedUntil;
    private Boolean isVerified;
    private String badgeLabel;

    private List<OrgMemberDto> members;
    private String ownerName;
}
