package com.tcon.learning_management_service.worksheet.mapper;

import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetQuestionRef;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;

// ✅ ADDED IMPORTS (from second code)
import com.tcon.learning_management_service.worksheet.integration.CourseClient;
import com.tcon.learning_management_service.worksheet.integration.dto.SubjectDto;
import com.tcon.learning_management_service.worksheet.integration.dto.TopicDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor // ✅ ADDED
public class WorksheetMapper {

    // ✅ ADDED
    private final CourseClient courseClient;

    /*
     * ===============================
     * CREATE DTO → ENTITY
     * ===============================
     */

    public Worksheet toEntity(CreateWorksheetRequest request, String createdBy) {

        Worksheet worksheet = new Worksheet();

        worksheet.setTitle(request.getTitle());
        worksheet.setSubjectId(request.getSubjectId());
        worksheet.setGradeId(request.getGradeId());
        worksheet.setTopicId(request.getTopicId());

        worksheet.setCreatedBy(createdBy);
        worksheet.setStatus(WorksheetStatus.DRAFT);

        worksheet.setCreatedAt(LocalDateTime.now());
        worksheet.setUpdatedAt(LocalDateTime.now());

        worksheet.setCurrentVersion(null);

        return worksheet;
    }

    /*
     * ===============================
     * ENTITY → BASIC RESPONSE
     * ===============================
     */

    public WorksheetResponse toResponse(Worksheet worksheet) {

        WorksheetResponse response = new WorksheetResponse();

        response.setId(worksheet.getId());
        response.setTitle(worksheet.getTitle());
        response.setCurrentVersion(worksheet.getCurrentVersion());
        response.setStatus(worksheet.getStatus().name());

        return response;
    }

    /*
     * ===============================
     * ENTITY → SUMMARY RESPONSE
     * ===============================
     */

    public WorksheetSummaryResponse toSummary(Worksheet worksheet) {

        WorksheetSummaryResponse response =
                new WorksheetSummaryResponse();

        response.setId(worksheet.getId());
        response.setTitle(worksheet.getTitle());
        response.setSubjectId(worksheet.getSubjectId());
        response.setGradeId(worksheet.getGradeId());

        // ✅ ADDED (new field from second code)
        response.setTopicId(worksheet.getTopicId());

        response.setStatus(worksheet.getStatus().name());

        // ✅ ADDED BLOCK (no removal of existing logic)
        try {
            // SUBJECT
            List<SubjectDto> subjects =
                    courseClient.getSubjectsByGrade(worksheet.getGradeId());

            String subjectName = subjects.stream()
                    .filter(s -> s.getId().equals(worksheet.getSubjectId()))
                    .map(SubjectDto::getName)
                    .findFirst()
                    .orElse(null);

            response.setSubjectName(
                    subjectName != null ? subjectName : worksheet.getSubjectId()
            );

            // TOPIC
            List<TopicDto> topics =
                    courseClient.getTopicsBySubject(worksheet.getSubjectId());

            String topicName = topics.stream()
                    .filter(t -> t.getId().equals(worksheet.getTopicId()))
                    .map(TopicDto::getName)
                    .findFirst()
                    .orElse(null);

            response.setTopicName(
                    topicName != null ? topicName : worksheet.getTopicId()
            );

            // GRADE
            var grades = courseClient.getGrades();

            String gradeName = grades.stream()
                    .filter(g -> g.getId().equals(worksheet.getGradeId()))
                    .map(g -> g.getName())
                    .findFirst()
                    .orElse(null);

            response.setGradeName(
                    gradeName != null ? gradeName : worksheet.getGradeId()
            );

        } catch (Exception e) {
            // fallback
            response.setSubjectName(worksheet.getSubjectId());
            response.setTopicName(worksheet.getTopicId());
            response.setGradeName(worksheet.getGradeId());
        }

        return response;
    }

    /*
     * ===============================
     * VERSION → DETAIL RESPONSE
     * ===============================
     */

    public WorksheetDetailResponse toDetailResponse(
            Worksheet worksheet,
            WorksheetVersion version
    ) {

        WorksheetDetailResponse response =
                new WorksheetDetailResponse();

        response.setWorksheetId(worksheet.getId());
        response.setTitle(worksheet.getTitle());
        response.setVersion(version.getVersionNumber());

        response.setQuestions(
                version.getQuestions()
                        .stream()
                        .map(this::mapQuestion)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private WorksheetDetailResponse.QuestionResponse
    mapQuestion(WorksheetQuestionRef ref) {

        WorksheetDetailResponse.QuestionResponse qr =
                new WorksheetDetailResponse.QuestionResponse();

        qr.setQuestionMasterId(ref.getQuestionMasterId());
        qr.setQuestionVersionId(ref.getQuestionVersionId());
        qr.setOrderIndex(ref.getOrderIndex());
        qr.setMarks(ref.getMarks());

        return qr;
    }
}