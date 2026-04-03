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

import java.time.LocalDateTime;

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
        Worksheet worksheet = worksheetRepository
                .findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        validator.validateWorksheetExists(worksheet);

        // 🔥 NEW: VALIDATE QUESTIONS EXIST
        if (worksheet.getHasQuestions() == null || !worksheet.getHasQuestions()) {
            throw new RuntimeException("Cannot publish worksheet without uploading questions");
        }

        // 2️⃣ Fetch Latest Version
        WorksheetVersion version = versionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        validator.validateVersionExists(version);

        // 🔥 EXTRA SAFETY
        if (version.getQuestionCount() == null || version.getQuestionCount() == 0) {
            throw new RuntimeException("Worksheet version has no questions");
        }

        // 3️⃣ Validate publish rules
        validator.validatePublishable(version);

        // 4️⃣ Lock Version
        version.setStatus(WorksheetStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());

        versionService.lockPublishedVersion(version);
        versionRepository.save(version);

        // 5️⃣ Update Worksheet Pointer
        worksheet.setCurrentVersion(version.getVersionNumber());
        worksheet.setStatus(WorksheetStatus.PUBLISHED);
        worksheet.setUpdatedAt(LocalDateTime.now());

        // 🔥 Initialize Review Flow
        worksheet.setReviewStatus(
                worksheet.getReviewStatus() == null
                        ? com.tcon.learning_management_service.worksheet.entity.ReviewStatus.PENDING
                        : worksheet.getReviewStatus()
        );

        worksheet.setReviewedBy(null);
        worksheet.setReviewComments(null);
        worksheet.setReviewedAt(null);

        worksheetRepository.save(worksheet);

        // 6️⃣ Emit Event
        eventPublisher.publishWorksheetPublished(
                worksheet.getId(),
                version.getVersionNumber(),
                worksheet.getCreatedBy()
        );
    }
}