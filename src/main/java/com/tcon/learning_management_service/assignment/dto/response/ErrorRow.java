package com.tcon.learning_management_service.assignment.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ErrorRow {
    private int rowNumber;
    private String errorMessage;
}