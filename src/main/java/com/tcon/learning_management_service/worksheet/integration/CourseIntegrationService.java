package com.tcon.learning_management_service.worksheet.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CourseIntegrationService {

    /*
     * Validate academic hierarchy
     */
    public void validateSubjectAndGrade(
            String subjectId,
            String gradeId,
            String topicId
    ) {

        log.info(
                "Validating subject={}, grade={}, topic={}",
                subjectId,
                gradeId,
                topicId
        );

        // FUTURE:
        // courseServiceClient.validate(...)
    }
}