package com.tcon.learning_management_service.course.service;
import com.tcon.learning_management_service.course.entity.Grade;
import com.tcon.learning_management_service.course.entity.Subject;
import com.tcon.learning_management_service.course.entity.Topic;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import com.tcon.learning_management_service.course.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculumImportService {

    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public ImportResult importFromExcel(MultipartFile file) {
        ImportResult result = new ImportResult();

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                result.getErrors().add("No sheet found in Excel file");
                return result;
            }

            // Assume first row is header
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                result.getErrors().add("Sheet is empty");
                return result;
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> colIndex = mapHeaderColumns(headerRow);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    processRow(row, colIndex, result);
                } catch (Exception ex) {
                    log.warn("Failed to process row {}: {}", row.getRowNum(), ex.getMessage());
                    result.getErrors().add("Row " + row.getRowNum() + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to import curriculum from Excel", e);
            result.getErrors().add("Fatal error reading Excel: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Integer> mapHeaderColumns(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = cell.getStringCellValue();
            if (value != null) {
                String key = value.trim().toLowerCase();
                map.put(key, cell.getColumnIndex());
            }
        }
        // Expect these at least
        if (!map.containsKey("grade") || !map.containsKey("subject") || !map.containsKey("topic")) {
            throw new IllegalArgumentException("Header must contain Grade, Subject, Topic columns");
        }
        return map;
    }

    private void processRow(Row row, Map<String, Integer> colIndex, ImportResult result) {
        String gradeName = getStringCell(row, colIndex.get("grade"));
        String subjectName = getStringCell(row, colIndex.get("subject"));
        String topicName = getStringCell(row, colIndex.get("topic"));
        Integer durationMinutes = getIntegerCell(row, colIndex.getOrDefault("durationminutes", -1));

        if (isBlank(gradeName) || isBlank(subjectName) || isBlank(topicName)) {
            result.getSkippedRows().add(row.getRowNum());
            return;
        }

        gradeName = gradeName.trim();
        subjectName = subjectName.trim();
        topicName = topicName.trim();

        // 1) Grade
        Grade grade = gradeRepository.findByNameIgnoreCase(gradeName);
        if (grade == null) {
            grade = Grade.builder()
                    .name(gradeName)
                    .order(calculateNextOrder())
                    .isActive(true)
                    .build();
            grade = gradeRepository.save(grade);
            result.setGradesCreated(result.getGradesCreated() + 1);
        }

        // 2) Subject (unique per grade+name)
        Subject subject = subjectRepository
                .findByGradeIdAndNameIgnoreCase(grade.getId(), subjectName)
                .orElse(null);
        if (subject == null) {
            subject = Subject.builder()
                    .gradeId(grade.getId())
                    .name(subjectName)
                    .description(null)
                    .isActive(true)
                    .build();
            subject = subjectRepository.save(subject);
            result.setSubjectsCreated(result.getSubjectsCreated() + 1);
        }

        // 3) Topic (unique per subject+name)
        Topic topic = topicRepository
                .findBySubjectIdAndNameIgnoreCase(subject.getId(), topicName)
                .orElse(null);
        if (topic == null) {
            topic = Topic.builder()
                    .subjectId(subject.getId())
                    .name(topicName)
                    .description(null)
                    .isActive(true)
                    .durationMinutes(durationMinutes)
                    .build();
            // If you add duration field later, set it here.
            topic = topicRepository.save(topic);
            result.setTopicsCreated(result.getTopicsCreated() + 1);
        } else {
            if (durationMinutes != null) {
                topic.setDurationMinutes(durationMinutes);
            }
            topic = topicRepository.save(topic);
            result.setTopicsUpdated(result.getTopicsUpdated() + 1);
        }

        result.setRowsProcessed(result.getRowsProcessed() + 1);
    }

    private int calculateNextOrder() {
        List<Grade> all = gradeRepository.findAll();
        return all.stream()
                .map(Grade::getOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String getStringCell(Row row, Integer colIdx) {
        if (colIdx == null || colIdx < 0) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }

    private Integer getIntegerCell(Row row, Integer colIdx) {
        if (colIdx == null || colIdx < 0) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                String v = cell.getStringCellValue();
                return isBlank(v) ? null : Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Simple result DTO for frontend
    @lombok.Data
    public static class ImportResult {
        private int rowsProcessed;
        private int gradesCreated;
        private int subjectsCreated;
        private int topicsCreated;
        private int topicsUpdated;
        private List<Integer> skippedRows = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
    }
}