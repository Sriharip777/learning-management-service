package com.tcon.learning_management_service.demo.controller;


import com.tcon.learning_management_service.demo.dto.DemoClassDto;
import com.tcon.learning_management_service.demo.dto.DemoLimitDto;
import com.tcon.learning_management_service.demo.service.DemoClassService;
import com.tcon.learning_management_service.demo.service.DemoLimitService;
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
@RequestMapping("/api/demos")
@RequiredArgsConstructor
public class DemoClassController {

    private final DemoClassService demoService;
    private final DemoLimitService limitService;

    @PostMapping
    public ResponseEntity<DemoClassDto> scheduleDemoClass(
            @RequestHeader("X-User-Id") String studentId,
            @RequestBody Map<String, Object> requestData) {

        String studentName = (String) requestData.get("studentName");
        String studentEmail = (String) requestData.get("studentEmail");
        String courseId = (String) requestData.get("courseId");
        String scheduledStartTimeStr = (String) requestData.get("scheduledStartTime");
        String studentNotes = (String) requestData.get("studentNotes");

        LocalDateTime scheduledStartTime = LocalDateTime.parse(scheduledStartTimeStr);

        DemoClassDto demo = demoService.scheduleDemoClass(
                studentId, studentName, studentEmail, courseId,
                scheduledStartTime, studentNotes);

        return ResponseEntity.status(HttpStatus.CREATED).body(demo);
    }

    @PostMapping("/{demoId}/confirm")
    public ResponseEntity<DemoClassDto> confirmDemoClass(
            @PathVariable String demoId,
            @RequestBody Map<String, String> meetingData) {

        String meetingUrl = meetingData.get("meetingUrl");
        String meetingId = meetingData.get("meetingId");
        String meetingPassword = meetingData.get("meetingPassword");

        DemoClassDto demo = demoService.confirmDemoClass(demoId, meetingUrl, meetingId, meetingPassword);
        return ResponseEntity.ok(demo);
    }

    @PostMapping("/{demoId}/start")
    public ResponseEntity<DemoClassDto> startDemoClass(
            @PathVariable String demoId,
            @RequestHeader("X-User-Id") String teacherId) {
        DemoClassDto demo = demoService.startDemoClass(demoId, teacherId);
        return ResponseEntity.ok(demo);
    }

    @PostMapping("/{demoId}/complete")
    public ResponseEntity<DemoClassDto> completeDemoClass(
            @PathVariable String demoId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, Object> feedbackData) {

        String teacherFeedback = (String) feedbackData.get("teacherFeedback");
        Integer teacherRating = (Integer) feedbackData.get("teacherRating");

        DemoClassDto demo = demoService.completeDemoClass(demoId, teacherId, teacherFeedback, teacherRating);
        return ResponseEntity.ok(demo);
    }

    @PostMapping("/{demoId}/feedback")
    public ResponseEntity<DemoClassDto> submitStudentFeedback(
            @PathVariable String demoId,
            @RequestHeader("X-User-Id") String studentId,
            @RequestBody Map<String, Integer> feedbackData) {

        Integer rating = feedbackData.get("rating");
        DemoClassDto demo = demoService.submitStudentFeedback(demoId, studentId, rating);
        return ResponseEntity.ok(demo);
    }

    @DeleteMapping("/{demoId}")
    public ResponseEntity<Map<String, String>> cancelDemoClass(
            @PathVariable String demoId,
            @RequestHeader("X-User-Id") String userId) {
        demoService.cancelDemoClass(demoId, userId);
        return ResponseEntity.ok(Map.of("message", "Demo class cancelled successfully"));
    }

    @GetMapping("/{demoId}")
    public ResponseEntity<DemoClassDto> getDemoClass(@PathVariable String demoId) {
        DemoClassDto demo = demoService.getDemoClass(demoId);
        return ResponseEntity.ok(demo);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<DemoClassDto>> getStudentDemos(@PathVariable String studentId) {
        List<DemoClassDto> demos = demoService.getStudentDemos(studentId);
        return ResponseEntity.ok(demos);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<DemoClassDto>> getTeacherDemos(@PathVariable String teacherId) {
        List<DemoClassDto> demos = demoService.getTeacherDemos(teacherId);
        return ResponseEntity.ok(demos);
    }

    @GetMapping("/limit/{studentId}")
    public ResponseEntity<DemoLimitDto> getDemoLimit(@PathVariable String studentId) {
        DemoLimitDto limit = limitService.getDemoLimit(studentId);
        return ResponseEntity.ok(limit);
    }

    @PostMapping("/limit/{studentId}/reset")
    public ResponseEntity<Map<String, String>> resetDemoLimit(@PathVariable String studentId) {
        limitService.resetDemoLimit(studentId);
        return ResponseEntity.ok(Map.of("message", "Demo limit reset successfully"));
    }

    @PostMapping("/limit/{studentId}/initialize")
    public ResponseEntity<DemoLimitDto> initializeDemoLimit(@PathVariable String studentId) {
        log.info("Initializing demo limit for student: {}", studentId);
        DemoLimitDto limit = limitService.initializeDemoLimit(studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(limit);
    }

}
