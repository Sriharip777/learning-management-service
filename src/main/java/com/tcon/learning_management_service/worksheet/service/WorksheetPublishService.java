package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.event.WorksheetEventPublisher;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorksheetPublishService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetValidator validator;
    private final WorksheetVersionService versionService;
    private final WorksheetEventPublisher eventPublisher;
    private final QuestionRepository questionRepository;

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

        // 2️⃣ Fetch Latest Version (ONLY ONCE ✅)
        WorksheetVersion version = versionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        validator.validateVersionExists(version);

        // 🔥 FIX: Validate using version question refs
        if (version.getQuestions() == null || version.getQuestions().isEmpty()) {
            throw new RuntimeException("Cannot publish worksheet without questions");
        }

        // ✅ ensure flag is correct
        worksheet.setHasQuestions(true);

        // ❗ Prevent publishing flagged worksheets
        if (worksheet.getReviewStatus() != null &&
                worksheet.getReviewStatus().name().equals("FLAGGED")) {

            throw new RuntimeException("Cannot publish flagged worksheet. Resolve issues first.");
        }

        // 🔥 EXTRA SAFETY
        if (version.getQuestionCount() == null || version.getQuestionCount() == 0) {
            throw new RuntimeException("Worksheet version has no questions");
        }

        // 3️⃣ Lock Version
        version.setStatus(WorksheetStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());

        versionService.lockPublishedVersion(version);
        versionRepository.save(version);

        // 4️⃣ Update Worksheet Pointer
        worksheet.setCurrentVersion(version.getVersionNumber());
        worksheet.setStatus(WorksheetStatus.PUBLISHED);
        worksheet.setUpdatedAt(LocalDateTime.now());

        worksheetRepository.save(worksheet);

        // 5️⃣ Emit Event
        eventPublisher.publishWorksheetPublished(
                worksheet.getId(),
                version.getVersionNumber(),
                worksheet.getCreatedBy()
        );
    }
}