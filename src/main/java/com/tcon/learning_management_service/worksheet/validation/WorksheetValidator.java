package com.tcon.learning_management_service.worksheet.validation;

import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import org.springframework.stereotype.Component;

@Component
public class WorksheetValidator {

    /*
     * ===============================
     * WORKSHEET EXISTENCE
     * ===============================
     */

    public void validateWorksheetExists(Worksheet worksheet) {

        if (worksheet == null) {
            throw new ResourceNotFoundException("Worksheet not found");
        }
    }

    /*
     * ===============================
     * EDIT VALIDATION
     * ===============================
     */

    public void validateEditable(Worksheet worksheet) {

        if (worksheet.getStatus() == WorksheetStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Published worksheet cannot be edited"
            );
        }

        if (worksheet.getStatus() == WorksheetStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Archived worksheet cannot be modified"
            );
        }
    }

    /*
     * ===============================
     * VERSION VALIDATION
     * ===============================
     */

    public void validateVersionExists(WorksheetVersion version) {

        if (version == null) {
            throw new ResourceNotFoundException(
                    "Worksheet version not found"
            );
        }
    }

    /*
     * ===============================
     * PUBLISH VALIDATION
     * ===============================
     */

    public void validatePublishable(WorksheetVersion version) {

        if (version.getQuestions() == null
                || version.getQuestions().isEmpty()) {

            throw new IllegalStateException(
                    "Cannot publish worksheet without questions"
            );
        }
    }

    /*
     * ===============================
     * STATUS TRANSITION VALIDATION
     * ===============================
     */

    public void validateStatusTransition(
            WorksheetStatus current,
            WorksheetStatus target
    ) {

        if (current == WorksheetStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Archived worksheet cannot change status"
            );
        }

        if (current == WorksheetStatus.PUBLISHED
                && target == WorksheetStatus.DRAFT) {

            throw new IllegalStateException(
                    "Published worksheet cannot move back to draft"
            );
        }
    }
}