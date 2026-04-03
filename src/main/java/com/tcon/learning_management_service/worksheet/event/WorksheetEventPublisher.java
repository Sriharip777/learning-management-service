package com.tcon.learning_management_service.worksheet.event;

import com.tcon.learning_management_service.worksheet.event.model.QuestionsFlaggedEvent;
import com.tcon.learning_management_service.worksheet.event.model.WorksheetPublishedEvent;
import com.tcon.learning_management_service.worksheet.event.model.WorksheetRejectedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorksheetEventPublisher {

    /*
     * KafkaTemplate will be injected later
     */

    /*
     * ======================================
     * PUBLISH WORKSHEET (EXISTING)
     * ======================================
     */
    public void publishWorksheetPublished(
            String worksheetId,
            Integer version,
            String publishedBy
    ) {

        WorksheetPublishedEvent event =
                new WorksheetPublishedEvent(
                        worksheetId,
                        version,
                        publishedBy,
                        LocalDateTime.now()
                );

        log.info("Publishing Worksheet Published Event: {}", event);

        // FUTURE:
        // kafkaTemplate.send("worksheet.published", event);
    }

    /*
     * ======================================
     * 🔥 NEW: WORKSHEET REJECTED EVENT
     * ======================================
     */
    public void publishWorksheetRejected(
            String worksheetId,
            String teacherId,
            String comments
    ) {

        WorksheetRejectedEvent event =
                new WorksheetRejectedEvent(
                        worksheetId,
                        teacherId,
                        comments
                );

        log.info("Publishing Worksheet Rejected Event: {}", event);

        // FUTURE:
        // kafkaTemplate.send("worksheet.rejected", event);
    }

    /*
     * ======================================
     * 🔥 NEW: QUESTIONS FLAGGED EVENT
     * ======================================
     */
    public void publishQuestionsFlagged(
            String worksheetId,
            String flaggedBy,
            String reason
    ) {

        QuestionsFlaggedEvent event =
                new QuestionsFlaggedEvent(
                        worksheetId,
                        flaggedBy,
                        reason,
                        LocalDateTime.now()
                );

        log.info("Publishing Questions Flagged Event: {}", event);

        // FUTURE:
        // kafkaTemplate.send("questions.flagged", event);
    }
}