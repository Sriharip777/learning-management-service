package com.tcon.learning_management_service.course.controller;

import com.tcon.learning_management_service.course.service.CurriculumImportService;
import com.tcon.learning_management_service.course.service.CurriculumImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumImportController {

    private final CurriculumImportService curriculumImportService;

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importCurriculum(@RequestParam("file") MultipartFile file) {
        log.info("📥 [CurriculumImport] Received file: {}", file.getOriginalFilename());
        ImportResult result = curriculumImportService.importFromExcel(file);
        log.info("✅ [CurriculumImport] rows={}, gradesCreated={}, subjectsCreated={}, topicsCreated={}, topicsUpdated={}",
                result.getRowsProcessed(),
                result.getGradesCreated(),
                result.getSubjectsCreated(),
                result.getTopicsCreated(),
                result.getTopicsUpdated()
        );
        return ResponseEntity.ok(result);
    }
}