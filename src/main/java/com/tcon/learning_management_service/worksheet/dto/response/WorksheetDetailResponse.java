package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class WorksheetDetailResponse {

    private String worksheetId;
    private String title;

    private Integer version;

    private List<QuestionResponse> questions;

    @Data
    public static class QuestionResponse {

        private String questionMasterId;
        private String questionVersionId;

        private Integer orderIndex;
        private Integer marks;
    }
}