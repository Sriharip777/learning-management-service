package com.tcon.learning_management_service.resource.service;
import com.tcon.learning_management_service.resource.client.IntegrationFileClient;
import com.tcon.learning_management_service.resource.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceStorageService {

    private final IntegrationFileClient integrationFileClient;

    public FileUploadResponse uploadPdf(MultipartFile file, String entityId) {
        log.info("Delegating file upload to integration-service: file={}, entityId={}",
                file.getOriginalFilename(), entityId);

        return integrationFileClient.uploadFile(
                file,
                "resource",
                entityId
        );
    }
}