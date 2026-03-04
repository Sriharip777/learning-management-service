package com.tcon.learning_management_service.worksheet.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssignmentIntegrationService {

    public void assignWorksheet(String worksheetId) {

        log.info(
                "Creating assignment from worksheet={}",
                worksheetId
        );

        // FUTURE FLOW:
        // assignmentClient.createAssignment(...)
    }
}