package com.tcon.learning_management_service.worksheet.event;

import com.tcon.learning_management_service.worksheet.event.model.WorksheetPublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorksheetEventListener {

    public void handleWorksheetPublished(
            WorksheetPublishedEvent event
    ) {

        log.info(
                "Worksheet Published Received -> {}",
                event
        );

        /*
         FUTURE USE:
         - analytics update
         - cache refresh
         - notification trigger
         */
    }
}