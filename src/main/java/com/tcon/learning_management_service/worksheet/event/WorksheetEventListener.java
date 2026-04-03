package com.tcon.learning_management_service.worksheet.event;

import com.tcon.learning_management_service.worksheet.event.model.WorksheetPublishedEvent;
import com.tcon.learning_management_service.worksheet.event.model.WorksheetRejectedEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorksheetEventListener {

    /*
     * ======================================
     * WORKSHEET PUBLISHED EVENT
     * ======================================
     */
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

    /*
     * ======================================
     * 🔥 NEW: WORKSHEET REJECTED EVENT
     * ======================================
     */
    public void handleWorksheetRejected(
            WorksheetRejectedEvent event
    ) {

        log.info(
                "🚨 Worksheet Rejected -> worksheetId={}, teacherId={}, comments={}",
                event.getWorksheetId(),
                event.getTeacherId(),
                event.getComments()
        );

        /*
         FUTURE USE:
         - Notify admin (email / UI)
         - Save notification in DB
         - Trigger alert system
        */
    }
}