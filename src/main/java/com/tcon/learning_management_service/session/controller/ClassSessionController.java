package com.tcon.learning_management_service.session.controller;

import com.tcon.learning_management_service.session.dto.SessionDto;
import com.tcon.learning_management_service.session.dto.SessionRescheduleRequest;
import com.tcon.learning_management_service.session.dto.SessionScheduleRequest;
import com.tcon.learning_management_service.session.service.ClassSessionService;
import com.tcon.learning_management_service.session.service.NoShowHandlingService;
import com.tcon.learning_management_service.session.service.SessionRescheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService sessionService;
    private final SessionRescheduleService rescheduleService;
    private final NoShowHandlingService noShowService;

    @PostMapping
    public ResponseEntity<SessionDto> scheduleSession(
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody SessionScheduleRequest request) {
        log.info("Scheduling session for teacher: {}", teacherId);
        SessionDto session = sessionService.scheduleSession(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDto> getSession(@PathVariable String sessionId) {
        SessionDto session = sessionService.getSession(sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<SessionDto>> getCourseSessions(@PathVariable String courseId) {
        List<SessionDto> sessions = sessionService.getCourseSessions(courseId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<SessionDto>> getTeacherSessions(@PathVariable String teacherId) {
        List<SessionDto> sessions = sessionService.getTeacherSessions(teacherId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<SessionDto>> getStudentSessions(@PathVariable String studentId) {
        List<SessionDto> sessions = sessionService.getStudentSessions(studentId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/teacher/{teacherId}/range")
    public ResponseEntity<List<SessionDto>> getTeacherSessionsInRange(
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<SessionDto> sessions = sessionService.getTeacherSessionsInDateRange(teacherId, start, end);
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<SessionDto> startSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId) {
        SessionDto session = sessionService.startSession(sessionId, teacherId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<SessionDto> completeSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String notes = requestBody != null ? requestBody.get("notes") : null;
        SessionDto session = sessionService.completeSession(sessionId, teacherId, notes);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<SessionDto> cancelSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");
        SessionDto session = sessionService.cancelSession(sessionId, teacherId, reason);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/reschedule")
    public ResponseEntity<SessionDto> rescheduleSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody SessionRescheduleRequest request) {
        SessionDto session = rescheduleService.rescheduleSession(sessionId, teacherId, request);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/attendance")
    public ResponseEntity<SessionDto> markAttendance(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> attendanceData) {
        String studentId = (String) attendanceData.get("studentId");
        String studentName = (String) attendanceData.get("studentName");
        String studentEmail = (String) attendanceData.get("studentEmail");
        Boolean attended = (Boolean) attendanceData.get("attended");

        SessionDto session = sessionService.markStudentAttendance(
                sessionId, studentId, studentName, studentEmail, attended);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/recording")
    public ResponseEntity<SessionDto> addRecording(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {
        String recordingUrl = requestBody.get("recordingUrl");
        SessionDto session = sessionService.addRecording(sessionId, teacherId, recordingUrl);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/no-show")
    public ResponseEntity<Map<String, String>> markAsNoShow(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String teacherId) {
        noShowService.markAsNoShow(sessionId, teacherId);
        return ResponseEntity.ok(Map.of("message", "Session marked as no-show"));
    }
}
