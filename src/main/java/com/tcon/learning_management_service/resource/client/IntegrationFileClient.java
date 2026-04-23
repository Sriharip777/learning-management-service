package com.tcon.learning_management_service.resource.client;

import com.tcon.learning_management_service.config.FeignMultipartSupportConfig;
import com.tcon.learning_management_service.resource.dto.FileUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "integration-service",
        contextId = "lmsIntegrationFileClient",
        configuration = FeignMultipartSupportConfig.class
)
public interface IntegrationFileClient {
    @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("entityType") String entityType,
            @RequestPart("entityId") String entityId
    );

    @DeleteMapping("/api/files/{fileId}")
    void deleteFile(@PathVariable("fileId") String fileId);
}