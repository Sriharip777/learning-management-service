package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/worksheets")
@RequiredArgsConstructor
public class WorksheetController {

    private final WorksheetQueryService queryService;

    /*
     * GET WORKSHEET DETAILS
     */
    @GetMapping("/{worksheetId}")
    public WorksheetDetailResponse getWorksheet(
            @PathVariable String worksheetId
    ) {
        return queryService.getWorksheetDetails(worksheetId);
    }
}