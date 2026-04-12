package com.tcon.learning_management_service.assignment.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class UploadResponse {
    private int successCount;
    private int errorCount;
    private List<ErrorRow> errors;
}