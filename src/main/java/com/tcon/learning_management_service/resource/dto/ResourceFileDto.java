package com.tcon.learning_management_service.resource.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFileDto {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;
}