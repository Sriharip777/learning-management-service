package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Question; // ✅ ADDED
import com.tcon.learning_management_service.assignment.service.AssignmentService;
import com.tcon.learning_management_service.assignment.service.QuestionService; // ✅ ADDED

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assignments")
@RequiredArgsConstructor
public class AssignmentAdminController {

    private final AssignmentService assignmentService;
    private final QuestionService questionService; // ✅ ADDED

    /**
     * Admin assigns assignment to teacher
     */
    @PostMapping("/{assignmentId}/assign-teacher")
    public Assignment assignToTeacher(
            @PathVariable String assignmentId,
            @RequestParam String teacherId) {

        return assignmentService.assignToTeacher(assignmentId, teacherId);
    }

    /**
     * 🔥 NEW: Admin view questions
     */
    @GetMapping("/{assignmentId}/questions")
    public List<Question> getQuestionsByAssignment(@PathVariable String assignmentId) {

        Assignment assignment = assignmentService.getAssignment(assignmentId);

        return questionService.getQuestionsByIds(assignment.getQuestionIds());
    }
}