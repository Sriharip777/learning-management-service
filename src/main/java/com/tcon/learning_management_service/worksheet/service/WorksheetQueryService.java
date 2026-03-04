package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorksheetQueryService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository versionRepository;
    private final WorksheetMapper mapper;
    private final WorksheetValidator validator;

    /*
     * ======================================
     * TEACHER VIEW
     * ======================================
     */

    public List<WorksheetSummaryResponse> getPublishedWorksheets(
            String subjectId,
            String gradeId
    ) {

        return worksheetRepository
                .findBySubjectIdAndGradeId(subjectId, gradeId)
                .stream()
                .filter(w -> w.getStatus() == WorksheetStatus.PUBLISHED)
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    /*
     * ======================================
     * WORKSHEET DETAIL
     * ======================================
     */

    public WorksheetDetailResponse getWorksheetDetails(
            String worksheetId
    ) {

        Worksheet worksheet =
                worksheetRepository.findById(worksheetId)
                        .orElse(null);

        validator.validateWorksheetExists(worksheet);

        WorksheetVersion version =
                versionRepository
                        .findByWorksheetIdAndVersionNumber(
                                worksheetId,
                                worksheet.getCurrentVersion()
                        )
                        .orElse(null);

        validator.validateVersionExists(version);

        return mapper.toDetailResponse(
                worksheet,
                version
        );
    }
}