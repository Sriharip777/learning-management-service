package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.service.AssignmentService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/assignments")
@RequiredArgsConstructor
public class AssignmentAdminController {

    private final AssignmentService assignmentService;

    /**
     * Admin assigns assignment to teacher
     */
    @PostMapping("/{assignmentId}/assign-teacher")
    public Assignment assignToTeacher(
            @PathVariable String assignmentId,
            @RequestParam String teacherId) {

        return assignmentService.assignToTeacher(assignmentId, teacherId);
    }
}