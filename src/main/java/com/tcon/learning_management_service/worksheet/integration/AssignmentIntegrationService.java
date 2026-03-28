package com.tcon.learning_management_service.worksheet.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AssignmentIntegrationService {

    public void assignWorksheet(
            String worksheetId,
            String teacherId,
            List<String> studentIds,
            LocalDateTime dueDate
    ) {

        log.info(
                "Creating assignment: worksheet={}, teacher={}, students={}, dueDate={}",
                worksheetId,
                teacherId,
                studentIds,
                dueDate
        );

        // 🔥 FUTURE FLOW:
        // assignmentClient.createAssignment(request);

        // For now just logging
    }
}