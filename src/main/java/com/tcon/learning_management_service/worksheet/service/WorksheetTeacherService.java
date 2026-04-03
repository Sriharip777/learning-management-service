package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.AssignWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.QuestionFlag;
import com.tcon.learning_management_service.worksheet.entity.ReviewStatus;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAssignment;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.event.WorksheetEventPublisher;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAssignmentRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetTeacherService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetAssignmentRepository assignmentRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetMapper worksheetMapper;
    private final WorksheetEventPublisher eventPublisher;

    /*
     * ======================================
     * 🔥 APPROVE WORKSHEET
     * ======================================
     */
    public void approveWorksheet(String worksheetId,
                                 String teacherId) {

        log.info("Approving worksheet: worksheetId={}, teacherId={}", worksheetId, teacherId);

        Worksheet worksheet = worksheetRepository.findById(worksheetId)
                .orElseThrow(() -> {
                    log.error("Worksheet not found: worksheetId={}", worksheetId);
                    return new RuntimeException("Worksheet not found");
                });

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            log.warn("Cannot approve non-published worksheet: worksheetId={}, status={}", worksheetId, worksheet.getStatus());
            throw new RuntimeException("Only published worksheets can be reviewed");
        }

        worksheet.setReviewStatus(ReviewStatus.APPROVED);
        worksheet.setReviewedBy(teacherId);
        worksheet.setReviewedAt(LocalDateTime.now());

        worksheetRepository.save(worksheet);

        log.info("Worksheet approved successfully: worksheetId={}, teacherId={}", worksheetId, teacherId);
    }

    /*
     * ======================================
     * 🔥 FLAG QUESTIONS (FINAL)
     * ======================================
     */
    public void flagQuestions(String worksheetId,
                              Integer versionNumber,
                              String teacherId,
                              List<QuestionFlag> flags) {

        log.info("Flagging questions: worksheetId={}, versionNumber={}, teacherId={}, flagCount={}",
                worksheetId, versionNumber, teacherId, flags.size());

        WorksheetVersion version = versionRepository
                .findByWorksheetIdAndVersionNumber(worksheetId, versionNumber)
                .orElseThrow(() -> {
                    log.error("Worksheet version not found: worksheetId={}, versionNumber={}", worksheetId, versionNumber);
                    return new RuntimeException("Worksheet version not found");
                });

        if (version.getStatus() != WorksheetStatus.PUBLISHED) {
            log.warn("Cannot flag questions in non-published version: worksheetId={}, status={}", worksheetId, version.getStatus());
            throw new RuntimeException("Only published worksheets can be flagged");
        }

        // Set metadata for each flagged question
        for (QuestionFlag flag : flags) {
            flag.setFlaggedBy(teacherId);
            flag.setFlaggedAt(LocalDateTime.now());
        }

        version.setFlaggedQuestions(flags);
        versionRepository.save(version);

        // 🔥 FIXED: publish FLAG event instead of rejected
        eventPublisher.publishQuestionsFlagged(
                worksheetId,
                teacherId,
                "Questions flagged for review"
        );

        log.info("Questions flagged successfully: worksheetId={}, flagCount={}", worksheetId, flags.size());
    }

    /*
     * ======================================
     * 🔥 VALIDATE BEFORE ASSIGN
     * ======================================
     */
    public void validateWorksheetBeforeAssign(String worksheetId) {

        log.info("Validating worksheet before assignment: worksheetId={}", worksheetId);

        Worksheet worksheet = worksheetRepository.findById(worksheetId)
                .orElseThrow(() -> {
                    log.error("Worksheet not found during validation: worksheetId={}", worksheetId);
                    return new RuntimeException("Worksheet not found");
                });

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            log.warn("Worksheet not published: worksheetId={}, status={}", worksheetId, worksheet.getStatus());
            throw new RuntimeException("Only published worksheets can be assigned");
        }

        if (worksheet.getReviewStatus() == null ||
                worksheet.getReviewStatus() != ReviewStatus.APPROVED) {
            log.warn("Worksheet not approved: worksheetId={}, reviewStatus={}", worksheetId, worksheet.getReviewStatus());
            throw new RuntimeException("Worksheet must be APPROVED before assignment");
        }

        log.info("Worksheet validation passed: worksheetId={}", worksheetId);
    }

    /*
     * ======================================
     * ASSIGN WORKSHEET
     * ======================================
     */
    public void assignWorksheet(AssignWorksheetRequest request) {

        log.info("Assigning worksheet: worksheetId={}, teacherId={}, studentCount={}",
                request.getWorksheetId(), request.getTeacherId(), request.getStudentIds().size());

        validateWorksheetBeforeAssign(request.getWorksheetId());

        int assignmentCount = 0;

        for (String studentId : request.getStudentIds()) {

            WorksheetAssignment assignment = new WorksheetAssignment();
            assignment.setWorksheetId(request.getWorksheetId());
            assignment.setTeacherId(request.getTeacherId());
            assignment.setStudentId(studentId);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDueDate(request.getDueDate());
            assignment.setCompleted(false);

            assignmentRepository.save(assignment);
            assignmentCount++;

            log.debug("Assignment created: worksheetId={}, studentId={}, dueDate={}",
                    request.getWorksheetId(), studentId, request.getDueDate());
        }

        log.info("Worksheet assigned successfully: worksheetId={}, totalAssignments={}",
                request.getWorksheetId(), assignmentCount);
    }

    /*
     * ======================================
     * 🔥 GET PENDING REVIEW WORKSHEETS
     * ======================================
     */
    public List<WorksheetSummaryResponse> getPendingReviewWorksheets(
            String subjectId,
            String gradeId,
            String topicId
    ) {

        log.info("Fetching pending review worksheets: subjectId={}, gradeId={}, topicId={}",
                subjectId, gradeId, topicId);

        List<Worksheet> worksheets =
                worksheetRepository.findByStatusAndReviewStatus(
                        WorksheetStatus.PUBLISHED,
                        ReviewStatus.PENDING
                );

        List<WorksheetSummaryResponse> result = worksheets.stream()
                .filter(w ->
                        w.getSubjectId().equals(subjectId) &&
                                w.getGradeId().equals(gradeId) &&
                                (topicId == null || w.getTopicId().equals(topicId))
                )
                .map(worksheetMapper::toSummary)
                .toList();

        log.info("Found {} pending review worksheets", result.size());

        return result;
    }

    /*
     * ======================================
     * 🔥 GET WORKSHEETS BY REVIEW STATUS
     * ======================================
     */
    public List<WorksheetSummaryResponse> getWorksheetsByReviewStatus(
            String subjectId,
            String gradeId,
            String topicId,
            ReviewStatus reviewStatus
    ) {

        log.info("Fetching worksheets by review status: subjectId={}, gradeId={}, topicId={}, reviewStatus={}",
                subjectId, gradeId, topicId, reviewStatus);

        List<Worksheet> worksheets =
                worksheetRepository.findByStatusAndReviewStatus(
                        WorksheetStatus.PUBLISHED,
                        reviewStatus
                );

        List<WorksheetSummaryResponse> result = worksheets.stream()
                .filter(w ->
                        w.getSubjectId().equals(subjectId) &&
                                w.getGradeId().equals(gradeId) &&
                                (topicId == null || w.getTopicId().equals(topicId))
                )
                .map(worksheetMapper::toSummary)
                .toList();

        log.info("Found {} worksheets with review status={}", result.size(), reviewStatus);

        return result;
    }

    /*
     * ======================================
     * 🔥 ADDED: SAFE ASSIGN (DUPLICATE PREVENTION)
     * ======================================
     */
    public void assignWorksheetSafe(AssignWorksheetRequest request) {

        log.info("Safe assign (duplicate protected): worksheetId={}, teacherId={}",
                request.getWorksheetId(), request.getTeacherId());

        validateWorksheetBeforeAssign(request.getWorksheetId());

        for (String studentId : request.getStudentIds()) {

            boolean alreadyAssigned = assignmentRepository
                    .findByStudentId(studentId)
                    .stream()
                    .anyMatch(a -> a.getWorksheetId().equals(request.getWorksheetId()));

            if (alreadyAssigned) {
                log.warn("⚠️ Already assigned to student: {}", studentId);
                continue;
            }

            WorksheetAssignment assignment = new WorksheetAssignment();
            assignment.setWorksheetId(request.getWorksheetId());
            assignment.setTeacherId(request.getTeacherId());
            assignment.setStudentId(studentId);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDueDate(request.getDueDate());
            assignment.setCompleted(false);

            assignmentRepository.save(assignment);

            log.info("✅ Safely assigned to student: {}", studentId);
        }
    }
}