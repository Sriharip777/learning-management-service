package com.tcon.learning_management_service.resource.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private String uploadedAt;
    private String entityType;
    private String entityId;
}