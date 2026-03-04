package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.AddQuestionRequest;
import com.tcon.learning_management_service.worksheet.entity.*;
import com.tcon.learning_management_service.worksheet.integration.QuestionBankIntegrationService;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorksheetVersionService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetValidator validator;
    private final QuestionBankIntegrationService questionBankIntegrationService;

    /*
     * ======================================
     * ADD QUESTIONS
     * ======================================
     */

    public WorksheetVersion addQuestions(AddQuestionRequest request) {

        Worksheet worksheet =
                worksheetRepository.findById(request.getWorksheetId())
                        .orElse(null);

        validator.validateWorksheetExists(worksheet);

        WorksheetVersion version =
                getOrCreateEditableVersion(worksheet);

        List<WorksheetQuestionRef> refs = new ArrayList<>();

        request.getQuestions().forEach(q -> {

            // validate question exists in QuestionBank
            questionBankIntegrationService.validateQuestion(
                    q.getQuestionMasterId(),
                    q.getQuestionVersionId()
            );

            WorksheetQuestionRef ref = new WorksheetQuestionRef();

            ref.setQuestionMasterId(q.getQuestionMasterId());
            ref.setQuestionVersionId(q.getQuestionVersionId());
            ref.setOrderIndex(q.getOrderIndex());
            ref.setMarks(q.getMarks());

            refs.add(ref);
        });

        version.setQuestions(refs);
        version.setQuestionCount(refs.size());

        return versionRepository.save(version);
    }

    /*
     * ======================================
     * GET OR CREATE EDITABLE VERSION
     * ======================================
     */

    private WorksheetVersion getOrCreateEditableVersion(
            Worksheet worksheet
    ) {

        if (worksheet.getCurrentVersion() == null) {
            return createVersion(worksheet, 1);
        }

        WorksheetVersion latest =
                versionRepository
                        .findTopByWorksheetIdOrderByVersionNumberDesc(
                                worksheet.getId()
                        )
                        .orElse(null);

        validator.validateVersionExists(latest);

        // if already published → clone
        if (latest.getStatus() == WorksheetStatus.PUBLISHED) {
            return cloneVersion(latest);
        }

        return latest;
    }

    /*
     * ======================================
     * CREATE VERSION
     * ======================================
     */

    public WorksheetVersion createVersion(
            Worksheet worksheet,
            int versionNumber
    ) {

        WorksheetVersion version = new WorksheetVersion();

        version.setWorksheetId(worksheet.getId());
        version.setVersionNumber(versionNumber);
        version.setStatus(WorksheetStatus.DRAFT);
        version.setCreatedAt(LocalDateTime.now());
        version.setQuestions(new ArrayList<>());

        return versionRepository.save(version);
    }

    /*
     * ======================================
     * CLONE VERSION
     * ======================================
     */

    public WorksheetVersion cloneVersion(
            WorksheetVersion oldVersion
    ) {

        WorksheetVersion newVersion = new WorksheetVersion();

        newVersion.setWorksheetId(oldVersion.getWorksheetId());
        newVersion.setVersionNumber(
                oldVersion.getVersionNumber() + 1
        );

        newVersion.setStatus(WorksheetStatus.DRAFT);
        newVersion.setCreatedAt(LocalDateTime.now());

        // clone questions safely
        newVersion.setQuestions(
                new ArrayList<>(oldVersion.getQuestions())
        );

        newVersion.setQuestionCount(
                oldVersion.getQuestionCount()
        );

        return versionRepository.save(newVersion);
    }

    /*
     * ======================================
     * LOCK PUBLISHED VERSION
     * ======================================
     */

    public void lockPublishedVersion(WorksheetVersion version) {

        version.setStatus(WorksheetStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());

        versionRepository.save(version);
    }
}