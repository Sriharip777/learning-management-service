package com.tcon.learning_management_service.worksheet.event;

import com.tcon.learning_management_service.worksheet.event.model.WorksheetPublishedEvent;
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

        log.info("Publishing Worksheet Event: {}", event);

        // FUTURE:
        // kafkaTemplate.send("worksheet.published", event);
    }
}