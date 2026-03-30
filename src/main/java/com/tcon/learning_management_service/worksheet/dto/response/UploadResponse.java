package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponse {

    private int successCount;
    private int failedCount;
    private List<ErrorRow> errors;
}