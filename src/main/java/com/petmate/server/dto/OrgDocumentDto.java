package com.petmate.server.dto;

import lombok.Data;

@Data
public class OrgDocumentDto {
    private Long id;
    private String docType;
    private String fileUrl;
    private String fileName;
}
