package com.tcon.learning_management_service.resource.client;

import com.tcon.learning_management_service.resource.dto.FileUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "integration-service", contextId = "lmsIntegrationFileClient")
public interface IntegrationFileClient {

    @PostMapping(value = "/api/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileUploadResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            // ✅ NO @RequestHeader("X-User-Id") here — FeignClientConfiguration forwards it
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") String entityId
    );

    @DeleteMapping("/api/files/{fileId}")
    void deleteFile(@PathVariable("fileId") String fileId);
    // ✅ NO @RequestHeader("X-User-Id") here either
}