package com.tcon.learning_management_service.tracking.service;

import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.repository.SubmissionRepository;
import com.tcon.learning_management_service.booking.dto.BookingDto;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.service.BookingService;
import com.tcon.learning_management_service.session.dto.SessionDto;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionParticipant;
import com.tcon.learning_management_service.session.service.ClassSessionService;
import com.tcon.learning_management_service.tracking.dto.TeacherTrackingResponseDto;
import com.tcon.learning_management_service.tracking.dto.TeacherTrackingStudentDto;
import com.tcon.learning_management_service.tracking.dto.TeacherTrackingSummaryDto;
import com.tcon.learning_management_service.tracking.dto.UpcomingClassDto;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherTrackingService {

    private final BookingService bookingService;
    private final ClassSessionService classSessionService;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final WorksheetAttemptRepository worksheetAttemptRepository;

    public TeacherTrackingResponseDto getTeacherTracking(String teacherId) {
        log.info("Building teacher tracking dashboard for teacherId={}", teacherId);

        List<BookingDto> teacherBookings = bookingService.getTeacherBookings(teacherId);
        List<SessionDto> teacherSessions = classSessionService.getTeacherSessions(teacherId);
        List<Assignment> teacherAssignments = assignmentRepository.findByTeacherId(teacherId);

        Map<String, StudentSeed> studentMap = buildTeacherStudentMap(teacherBookings, teacherSessions);

        List<TeacherTrackingStudentDto> students = studentMap.values().stream()
                .map(seed -> buildStudentMetrics(seed, teacherSessions, teacherAssignments))
                .sorted(Comparator.comparing(
                        TeacherTrackingStudentDto::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        int totalStudents = students.size();

        int needAttentionCount = (int) students.stream()
                .filter(s -> "needs_attention".equalsIgnoreCase(s.getStatus()))
                .count();

        int onTrackCount = totalStudents - needAttentionCount;

        int avgProgressPercent = totalStudents == 0
                ? 0
                : (int) Math.round(
                students.stream()
                        .mapToInt(this::calculateProgressPercent)
                        .average()
                        .orElse(0)
        );

        List<UpcomingClassDto> upcomingClasses = buildUpcomingClasses(teacherSessions, studentMap);

        return TeacherTrackingResponseDto.builder()
                .summary(
                        TeacherTrackingSummaryDto.builder()
                                .totalStudents(totalStudents)
                                .onTrackCount(onTrackCount)
                                .needAttentionCount(needAttentionCount)
                                .avgProgressPercent(avgProgressPercent)
                                .build()
                )
                .students(students)
                .upcomingClasses(upcomingClasses)
                .build();
    }

    private Map<String, StudentSeed> buildTeacherStudentMap(
            List<BookingDto> teacherBookings,
            List<SessionDto> teacherSessions
    ) {
        Map<String, StudentSeed> map = new LinkedHashMap<>();

        for (BookingDto booking : teacherBookings) {
            if (booking.getStudentId() == null || booking.getStudentId().isBlank()) {
                continue;
            }

            if (booking.getStatus() == BookingStatus.REJECTED || booking.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }

            StudentSeed existing = map.get(booking.getStudentId());

            String derivedCourse = firstNonBlank(booking.getSubject(), booking.getCourseId(), "General Course");
            String derivedLastActive = booking.getUpdatedAt() != null
                    ? booking.getUpdatedAt().toString()
                    : booking.getBookedAt() != null ? booking.getBookedAt().toString() : null;

            if (existing == null) {
                map.put(
                        booking.getStudentId(),
                        StudentSeed.builder()
                                .studentId(booking.getStudentId())
                                .name(firstNonBlank(booking.getStudentName(), "Student"))
                                .email(booking.getStudentEmail())
                                .course(derivedCourse)
                                .lastActive(derivedLastActive)
                                .build()
                );
            } else {
                if (isBlank(existing.getName()) && !isBlank(booking.getStudentName())) {
                    existing.setName(booking.getStudentName());
                }
                if (isBlank(existing.getEmail()) && !isBlank(booking.getStudentEmail())) {
                    existing.setEmail(booking.getStudentEmail());
                }
                if (isBlank(existing.getCourse()) && !isBlank(derivedCourse)) {
                    existing.setCourse(derivedCourse);
                }
                if (isBlank(existing.getLastActive()) && !isBlank(derivedLastActive)) {
                    existing.setLastActive(derivedLastActive);
                }
            }
        }

        for (SessionDto session : teacherSessions) {
            if (session.getParticipants() == null) {
                continue;
            }

            for (SessionParticipant participant : session.getParticipants()) {
                if (participant.getStudentId() == null || participant.getStudentId().isBlank()) {
                    continue;
                }

                StudentSeed existing = map.get(participant.getStudentId());
                String derivedCourse = firstNonBlank(session.getTitle(), session.getCourseId(), "General Course");
                String derivedLastActive = session.getUpdatedAt() != null
                        ? session.getUpdatedAt().toString()
                        : session.getScheduledStartTime() != null ? session.getScheduledStartTime().toString() : null;

                if (existing == null) {
                    map.put(
                            participant.getStudentId(),
                            StudentSeed.builder()
                                    .studentId(participant.getStudentId())
                                    .name(firstNonBlank(participant.getStudentName(), "Student"))
                                    .email(participant.getStudentEmail())
                                    .course(derivedCourse)
                                    .lastActive(derivedLastActive)
                                    .build()
                    );
                } else {
                    if (isBlank(existing.getName()) && !isBlank(participant.getStudentName())) {
                        existing.setName(participant.getStudentName());
                    }
                    if (isBlank(existing.getEmail()) && !isBlank(participant.getStudentEmail())) {
                        existing.setEmail(participant.getStudentEmail());
                    }
                    if (isBlank(existing.getCourse()) && !isBlank(derivedCourse)) {
                        existing.setCourse(derivedCourse);
                    }
                    if (isBlank(existing.getLastActive()) && !isBlank(derivedLastActive)) {
                        existing.setLastActive(derivedLastActive);
                    }
                }
            }
        }

        return map;
    }

    private TeacherTrackingStudentDto buildStudentMetrics(
            StudentSeed seed,
            List<SessionDto> teacherSessions,
            List<Assignment> teacherAssignments
    ) {
        String studentId = seed.getStudentId();

        List<SessionDto> studentSessions = teacherSessions.stream()
                .filter(session -> belongsToStudent(session, studentId))
                .filter(session -> session.getStatus() != ClassStatus.CANCELLED)
                .toList();

        int totalHours = studentSessions.stream()
                .map(SessionDto::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum() / 60;

        int attendedHours = studentSessions.stream()
                .filter(session -> didAttend(session, studentId))
                .map(SessionDto::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum() / 60;

        long attendedSessions = studentSessions.stream()
                .filter(session -> didAttend(session, studentId))
                .count();

        int attendancePercent = studentSessions.isEmpty()
                ? 0
                : (int) Math.round((attendedSessions * 100.0) / studentSessions.size());

        List<Assignment> studentAssignments = teacherAssignments.stream()
                .filter(a -> a.getStudentIds() != null && a.getStudentIds().contains(studentId))
                .toList();

        int totalHomework = studentAssignments.size();

        int homeworkCompleted = (int) studentAssignments.stream()
                .map(Assignment::getId)
                .filter(assignmentId -> submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId).isPresent())
                .count();

        Integer worksheetAverage = calculateWorksheetAverage(studentId);

        int classesRemaining = (int) studentSessions.stream()
                .filter(session -> session.getScheduledStartTime() != null)
                .filter(session -> session.getScheduledStartTime().isAfter(Instant.now()))
                .count();

        String status = resolveStatus(attendancePercent, homeworkCompleted, totalHomework, worksheetAverage);

        return TeacherTrackingStudentDto.builder()
                .id(studentId)
                .studentId(studentId)
                .name(firstNonBlank(seed.getName(), "Student"))
                .email(seed.getEmail())
                .course(firstNonBlank(seed.getCourse(), "General Course"))
                .hoursAttended(attendedHours)
                .totalHours(totalHours)
                .attendance(attendancePercent)
                .homeworkCompleted(homeworkCompleted)
                .totalHomework(totalHomework)
                .worksheetScore(worksheetAverage)
                .classesRemaining(classesRemaining)
                .status(status)
                .lastActive(seed.getLastActive())
                .build();
    }

    private Integer calculateWorksheetAverage(String studentId) {
        List<WorksheetAttempt> attempts = worksheetAttemptRepository.findByStudentId(studentId);

        if (attempts == null || attempts.isEmpty()) {
            return 0;
        }

        double average = attempts.stream()
                .filter(attempt -> attempt.getTotalQuestions() != null && attempt.getTotalQuestions() > 0)
                .mapToDouble(attempt -> {
                    int correct = attempt.getCorrectAnswers() != null ? attempt.getCorrectAnswers() : 0;
                    return (correct * 100.0) / attempt.getTotalQuestions();
                })
                .average()
                .orElse(0.0);

        return (int) Math.round(average);
    }

    private List<UpcomingClassDto> buildUpcomingClasses(
            List<SessionDto> teacherSessions,
            Map<String, StudentSeed> studentMap
    ) {
        List<UpcomingClassDto> rows = new ArrayList<>();

        teacherSessions.stream()
                .filter(session -> session.getScheduledStartTime() != null)
                .filter(session -> session.getScheduledStartTime().isAfter(Instant.now()))
                .filter(session -> session.getStatus() == ClassStatus.SCHEDULED || session.getStatus() == ClassStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(SessionDto::getScheduledStartTime))
                .limit(10)
                .forEach(session -> {
                    if (session.getParticipants() != null && !session.getParticipants().isEmpty()) {
                        for (SessionParticipant participant : session.getParticipants()) {
                            StudentSeed seed = studentMap.get(participant.getStudentId());
                            rows.add(
                                    UpcomingClassDto.builder()
                                            .id(session.getId() + ":" + participant.getStudentId())
                                            .studentId(participant.getStudentId())
                                            .student(firstNonBlank(
                                                    participant.getStudentName(),
                                                    seed != null ? seed.getName() : null,
                                                    "Student"
                                            ))
                                            .subject(firstNonBlank(session.getTitle(), session.getDescription(), "Class"))
                                            .scheduledStartTime(session.getScheduledStartTime())
                                            .build()
                            );
                        }
                    }
                });

        return rows.stream().limit(5).toList();
    }

    private boolean belongsToStudent(SessionDto session, String studentId) {
        return session.getParticipants() != null
                && session.getParticipants().stream()
                .anyMatch(p -> studentId.equals(p.getStudentId()));
    }

    private boolean didAttend(SessionDto session, String studentId) {
        if (session.getParticipants() == null) {
            return false;
        }
        return session.getParticipants().stream()
                .filter(p -> studentId.equals(p.getStudentId()))
                .anyMatch(p -> Boolean.TRUE.equals(p.getAttended()));
    }

    private int calculateProgressPercent(TeacherTrackingStudentDto s) {
        int attendance = safeInt(s.getAttendance());
        int homework = s.getTotalHomework() == null || s.getTotalHomework() == 0
                ? 0
                : (int) Math.round((s.getHomeworkCompleted() * 100.0) / s.getTotalHomework());
        int worksheet = safeInt(s.getWorksheetScore());

        return (int) Math.round((attendance + homework + worksheet) / 3.0);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String resolveStatus(
            int attendance,
            int homeworkCompleted,
            int totalHomework,
            Integer worksheetAverage
    ) {
        int homeworkPercent = totalHomework == 0
                ? 0
                : (int) Math.round((homeworkCompleted * 100.0) / totalHomework);
        int worksheet = worksheetAverage == null ? 0 : worksheetAverage;

        if (attendance >= 95 && homeworkPercent >= 90 && worksheet >= 90) {
            return "excellent";
        }

        if (attendance < 75 || homeworkPercent < 60 || worksheet < 60) {
            return "needs_attention";
        }

        return "on_track";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Data
    @Builder
    private static class StudentSeed {
        private String studentId;
        private String name;
        private String email;
        private String course;
        private String lastActive;
    }
}