package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class QuestionUpdateRequest {

    private String questionMasterId;
    private String questionVersionId;

    private String questionText;
    private List<String> options;
    private String reason;
}