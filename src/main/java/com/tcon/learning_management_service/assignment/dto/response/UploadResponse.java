package com.tcon.learning_management_service.assignment.dto.response;

import com.tcon.learning_management_service.assignment.entity.Question; // ✅ ADDED
import lombok.Data;

import java.util.List;

@Data
public class UploadResponse {

    private int successCount;
    private int errorCount;
    private List<ErrorRow> errors;
    private List<Question> questions; // ✅ ADDED

    // ✅ NEW CONSTRUCTOR
    public UploadResponse(int successCount, int errorCount, List<ErrorRow> errors, List<Question> questions) {
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.errors = errors;
        this.questions = questions;
    }
}