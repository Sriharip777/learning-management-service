package com.tcon.learning_management_service.resource.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFile {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
}