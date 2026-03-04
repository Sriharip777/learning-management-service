package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.request.UpdateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;
import com.tcon.learning_management_service.worksheet.integration.CourseIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorksheetService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetMapper worksheetMapper;
    private final WorksheetValidator worksheetValidator;
    private final CourseIntegrationService courseIntegrationService;

    /*
     * =====================================
     * CREATE WORKSHEET
     * =====================================
     */

    public WorksheetResponse createWorksheet(
            CreateWorksheetRequest request,
            String createdBy
    ) {

        // Validate academic hierarchy
        courseIntegrationService.validateSubjectAndGrade(
                request.getSubjectId(),
                request.getGradeId(),
                request.getTopicId()
        );

        // Map DTO → Entity
        Worksheet worksheet =
                worksheetMapper.toEntity(request, createdBy);

        // Save
        Worksheet saved =
                worksheetRepository.save(worksheet);

        return worksheetMapper.toResponse(saved);
    }

    /*
     * =====================================
     * UPDATE WORKSHEET
     * =====================================
     */

    public WorksheetResponse updateWorksheet(
            UpdateWorksheetRequest request
    ) {

        Worksheet worksheet =
                worksheetRepository.findById(
                        request.getWorksheetId()
                ).orElse(null);

        worksheetValidator.validateWorksheetExists(worksheet);
        worksheetValidator.validateEditable(worksheet);

        // Update allowed fields
        worksheet.setTitle(request.getTitle());
        worksheet.setUpdatedAt(LocalDateTime.now());

        Worksheet updated =
                worksheetRepository.save(worksheet);

        return worksheetMapper.toResponse(updated);
    }

    /*
     * =====================================
     * GET WORKSHEET
     * =====================================
     */

    public WorksheetResponse getWorksheet(String worksheetId) {

        Worksheet worksheet =
                worksheetRepository.findById(worksheetId)
                        .orElse(null);

        worksheetValidator.validateWorksheetExists(worksheet);

        return worksheetMapper.toResponse(worksheet);
    }
}