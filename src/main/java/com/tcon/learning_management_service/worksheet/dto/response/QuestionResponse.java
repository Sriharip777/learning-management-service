package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionResponse {

    private String id;
    private String questionText;
    private List<String> options;
}