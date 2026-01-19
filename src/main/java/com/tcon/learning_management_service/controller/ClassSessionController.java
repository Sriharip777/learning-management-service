package com.tcon.learning_management_service.controller;

import com.tcon.learning_management_service.dto.SessionDto;
import com.tcon.learning_management_service.dto.SessionScheduleRequest;
import com.tcon.learning_management_service.service.ClassSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionDto> scheduleSession(@Valid @RequestBody SessionScheduleRequest request) {
        SessionDto session = sessionService.scheduleSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PatchMapping("/{sessionId}/start")
    public ResponseEntity<SessionDto> startSession(@PathVariable String sessionId) {
        SessionDto session = sessionService.startSession(sessionId);
        return ResponseEntity.ok(session);
    }

    @PatchMapping("/{sessionId}/complete")
    public ResponseEntity<SessionDto> completeSession(@PathVariable String sessionId) {
        SessionDto session = sessionService.completeSession(sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<SessionDto>> getTeacherSessions(@PathVariable String teacherId) {
        List<SessionDto> sessions = sessionService.getSessionsByTeacher(teacherId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<SessionDto>> getStudentSessions(@PathVariable String studentId) {
        List<SessionDto> sessions = sessionService.getSessionsByStudent(studentId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<SessionDto>> getUpcomingSessions() {
        List<SessionDto> sessions = sessionService.getUpcomingSessions();
        return ResponseEntity.ok(sessions);
    }
}
