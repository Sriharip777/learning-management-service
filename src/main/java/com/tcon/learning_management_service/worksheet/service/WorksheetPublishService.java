package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.event.WorksheetEventPublisher;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorksheetPublishService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetValidator validator;
    private final WorksheetVersionService versionService;
    private final WorksheetEventPublisher eventPublisher;

    /*
     * ======================================
     * PUBLISH WORKSHEET
     * ======================================
     */
    public void publishWorksheet(String worksheetId) {

        // 1️⃣ Fetch Worksheet
        Worksheet worksheet =
                worksheetRepository.findById(worksheetId)
                        .orElse(null);

        validator.validateWorksheetExists(worksheet);

        // 2️⃣ Fetch Latest Version
        WorksheetVersion version =
                versionRepository
                        .findTopByWorksheetIdOrderByVersionNumberDesc(
                                worksheetId
                        )
                        .orElse(null);

        validator.validateVersionExists(version);

        // 3️⃣ Validate publish rules
        validator.validatePublishable(version);

        // 4️⃣ Lock Version
        versionService.lockPublishedVersion(version);

        // 5️⃣ Update Worksheet Pointer
        worksheet.setCurrentVersion(
                version.getVersionNumber()
        );

        worksheet.setStatus(WorksheetStatus.PUBLISHED);

        worksheetRepository.save(worksheet);

        // 6️⃣ Emit Event
        eventPublisher.publishWorksheetPublished(
                worksheet.getId(),
                version.getVersionNumber(),
                worksheet.getCreatedBy()
        );
    }
}