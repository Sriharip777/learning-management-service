package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.QuestionCreateRequest;
import com.tcon.learning_management_service.assignment.dto.response.UploadResponse;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * ✅ FIXED: Teacher creates question (manual)
     */
    @PostMapping
    public Question createQuestion(
            @RequestBody QuestionCreateRequest request) {

        return questionService.createQuestion(request);
    }


    /**
     * ✅ Upload questions via Excel
     */
    @PostMapping("/upload")
    public UploadResponse uploadQuestions(
            @RequestParam String teacherId,
            @RequestParam("file") MultipartFile file) {

        return questionService.uploadQuestionsFromExcel(teacherId, file);
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