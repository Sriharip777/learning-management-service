package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.SubmissionRequest;
import com.tcon.learning_management_service.assignment.entity.Answer;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final QuestionRepository questionRepository;


    public List<Answer> evaluate(
            SubmissionRequest request)
    {

        List<Question> questions =
                questionRepository
                        .findByAssignmentId(
                                request.getAssignmentId());

        List<Answer> answers = new ArrayList<>();

        request.getAnswers()
                .forEach(ans -> {

                    Question question =
                            questions.stream()
                                    .filter(q ->
                                            q.getId()
                                                    .equals(ans.getQuestionId()))
                                    .findFirst()
                                    .orElseThrow();

                    Answer answer = new Answer();

                    answer.setQuestionId(
                            ans.getQuestionId());

                    answer.setAnswer(
                            ans.getAnswer());

                    if(question.getCorrectAnswer()
                            .equals(ans.getAnswer()))
                    {
                        answer.setCorrect(true);
                        answer.setMarks(
                                question.getMarks());
                    }
                    else
                    {
                        answer.setCorrect(false);
                        answer.setMarks(0);
                    }

                    answers.add(answer);

                });

        return answers;

    }


    public int getTotalMarks(String assignmentId)
    {
        return questionRepository
                .findByAssignmentId(assignmentId)
                .stream()
                .mapToInt(Question::getMarks)
                .sum();
    }

}