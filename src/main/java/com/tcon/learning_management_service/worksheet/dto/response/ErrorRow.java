package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorRow {

    private int row;
    private String message;
}