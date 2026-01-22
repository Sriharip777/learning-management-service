package com.tcon.learning_management_service.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoLimitDto {
    private String studentId;
    private Integer totalDemosAllowed;
    private Integer demosUsed;
    private Integer demosRemaining;
    private Boolean canBookDemo;
    private String message;
}
