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


    // Teacher add question
    @PostMapping
    public Question addQuestion(
            @RequestBody QuestionCreateRequest request)
    {
        return questionService.addQuestion(request);
    }


    // Get assignment questions
    @GetMapping("/{assignmentId}")
    public List<Question> getQuestions(
            @PathVariable String assignmentId)
    {
        return questionService
                .getQuestions(assignmentId);
    }

}