package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.SubmissionRequest;
import com.tcon.learning_management_service.assignment.entity.Answer;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.repository.QuestionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;

    public List<Answer> evaluate(SubmissionRequest request) {

        // 1️⃣ Get assignment
        Assignment assignment = assignmentRepository.findById(
                        request.getAssignmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: "
                                        + request.getAssignmentId()));

        // 2️⃣ Fetch questions by questionIds
        List<Question> questions =
                questionRepository.findAllById(
                        assignment.getQuestionIds());

        List<Answer> answers = new ArrayList<>();

        request.getAnswers().forEach(ans -> {

            Question question = questions.stream()
                    .filter(q ->
                            q.getId().equals(ans.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Question not found in assignment"));

            Answer answer = new Answer();

            answer.setQuestionId(ans.getQuestionId());
            answer.setAnswer(ans.getAnswer());

            if (question.getCorrectAnswer()
                    .equals(ans.getAnswer())) {

                answer.setCorrect(true);
                answer.setMarks(1);   // ✅ Since marks removed
            } else {
                answer.setCorrect(false);
                answer.setMarks(0);
            }

            answers.add(answer);
        });

        return answers;
    }

    public int getTotalMarks(String assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));

        return assignment.getQuestionIds().size();  // 1 mark per question
    }
}