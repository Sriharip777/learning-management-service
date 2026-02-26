package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.QuestionCreateRequest;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Teacher creates question
     */
    @PostMapping
    public Question createQuestion(
            @RequestBody QuestionCreateRequest request) {
        return questionService.createQuestion(request);
    }

    /**
     * Get single question
     */
    @GetMapping("/{questionId}")
    public Question getQuestion(
            @PathVariable String questionId) {
        return questionService.getQuestion(questionId);
    }

    /**
     * Get all questions created by teacher
     */
    @GetMapping("/teacher/{teacherId}")
    public List<Question> getQuestionsByTeacher(
            @PathVariable String teacherId) {
        return questionService.getQuestionsByTeacher(teacherId);
    }
}