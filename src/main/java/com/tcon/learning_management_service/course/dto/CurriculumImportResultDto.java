package com.tcon.learning_management_service.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumImportResultDto {

    private int rowsProcessed;
    private int gradesCreated;
    private int subjectsCreated;
    private int topicsCreated;
    private int topicsUpdated;
}