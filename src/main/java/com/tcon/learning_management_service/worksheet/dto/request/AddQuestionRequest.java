package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AddQuestionRequest {

    private String worksheetId;

    private List<QuestionRequest> questions;

    @Data
    public static class QuestionRequest {

        private String questionMasterId;
        private String questionVersionId;

        private Integer orderIndex;
        private Integer marks;
    }
}