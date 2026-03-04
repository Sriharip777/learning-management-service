package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorksheetVersionRepository
        extends MongoRepository<WorksheetVersion, String> {

    List<WorksheetVersion> findByWorksheetId(String worksheetId);

    Optional<WorksheetVersion> findTopByWorksheetIdOrderByVersionNumberDesc(
            String worksheetId
    );

    Optional<WorksheetVersion> findByWorksheetIdAndVersionNumber(
            String worksheetId,
            Integer versionNumber
    );

    List<WorksheetVersion> findByStatus(WorksheetStatus status);
}