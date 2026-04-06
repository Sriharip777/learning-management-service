package com.tcon.learning_management_service.tracking.controller;

import com.tcon.learning_management_service.tracking.dto.TeacherTrackingResponseDto;
import com.tcon.learning_management_service.tracking.service.TeacherTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherTrackingController {

    private final TeacherTrackingService teacherTrackingService;

    @GetMapping("/tracking")
    public ResponseEntity<TeacherTrackingResponseDto> getTracking(
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(teacherTrackingService.getTeacherTracking(teacherId));
    }
}