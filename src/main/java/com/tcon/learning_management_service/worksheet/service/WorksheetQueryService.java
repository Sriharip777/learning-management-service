package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorksheetQueryService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetMapper mapper;
    private final WorksheetValidator validator;
    private final QuestionRepository questionRepository;

    /*
     * ======================================
     * TEACHER VIEW (SHOW ALL PUBLISHED)
     * ======================================
     */
    public List<WorksheetSummaryResponse> getPublishedWorksheets(
            String subjectId,
            String gradeId,
            String topicId
    ) {

        List<Worksheet> worksheets;

        if (topicId == null || topicId.isEmpty()) {
            worksheets = worksheetRepository
                    .findBySubjectIdAndGradeId(subjectId, gradeId)
                    .stream()
                    .filter(w -> w.getStatus() == WorksheetStatus.PUBLISHED)
                    .collect(Collectors.toList());
        } else {
            worksheets = worksheetRepository
                    .findByGradeIdAndSubjectIdAndTopicIdAndStatus(
                            gradeId,
                            subjectId,
                            topicId,
                            WorksheetStatus.PUBLISHED
                    );
        }

        // ✅ NO reviewStatus filter here (IMPORTANT)
        return worksheets.stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    /*
     * ======================================
     * WORKSHEET DETAIL
     * ======================================
     */
    public WorksheetDetailResponse getWorksheetDetails(String worksheetId) {

        Worksheet worksheet = worksheetRepository.findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        validator.validateWorksheetExists(worksheet);

        WorksheetVersion version = versionRepository
                .findByWorksheetIdAndVersionNumber(
                        worksheetId,
                        worksheet.getCurrentVersion()
                )
                .orElseThrow(() -> new RuntimeException("Version not found"));

        validator.validateVersionExists(version);

        // 🔥 Use mapper (keep original design)
        WorksheetDetailResponse response = mapper.toDetailResponse(worksheet, version);

        // 🔥 Enhance with real question data (colleague logic)
        response.getQuestions().forEach(q -> {

            var question = questionRepository
                    .findByQuestionMasterIdAndQuestionVersionId(
                            q.getQuestionMasterId(),
                            q.getQuestionVersionId()
                    )
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            q.setQuestionText(question.getQuestionText());
            q.setOptions(question.getOptions());
            q.setReason(question.getReason());
        });

        return response;
    }

    /*
     * ======================================
     * FILTERED WORKSHEET DETAILS
     * ======================================
     */
    public List<WorksheetDetailResponse> getPublishedWorksheetDetails(
            String gradeId,
            String subjectId,
            String topicId
    ) {

        List<Worksheet> worksheets;

        if (topicId == null || topicId.isEmpty()) {
            worksheets = worksheetRepository
                    .findBySubjectIdAndGradeId(subjectId, gradeId)
                    .stream()
                    .filter(w -> w.getStatus() == WorksheetStatus.PUBLISHED)
                    .collect(Collectors.toList());
        } else {
            worksheets = worksheetRepository
                    .findByGradeIdAndSubjectIdAndTopicIdAndStatus(
                            gradeId,
                            subjectId,
                            topicId,
                            WorksheetStatus.PUBLISHED
                    );
        }

        return worksheets.stream()
                .map(w -> {
                    WorksheetVersion version = versionRepository
                            .findByWorksheetIdAndVersionNumber(
                                    w.getId(),
                                    w.getCurrentVersion()
                            )
                            .orElseThrow(() -> new RuntimeException("Version not found"));

                    validator.validateVersionExists(version);

                    return mapper.toDetailResponse(w, version);
                })
                .collect(Collectors.toList());
    }

    /*
     * ======================================
     * LATEST WORKSHEETS (SHOW ALL PUBLISHED)
     * ======================================
     */
    public List<WorksheetSummaryResponse> getLatestPublishedWorksheets() {

        return worksheetRepository
                .findByStatusOrderByCreatedAtDesc(WorksheetStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }
}