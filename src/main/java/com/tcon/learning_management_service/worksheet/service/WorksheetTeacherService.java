package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.AssignWorksheetRequest;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAssignment;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAssignmentRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorksheetTeacherService {

    private final WorksheetRepository worksheetRepository;
    private final WorksheetAssignmentRepository assignmentRepository;

    public void assignWorksheet(AssignWorksheetRequest request) {

        // ✅ 1. Validate worksheet
        Worksheet worksheet = worksheetRepository.findById(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            throw new RuntimeException("Only published worksheets can be assigned");
        }

        // ✅ 2. Assign to students
        for (String studentId : request.getStudentIds()) {

            WorksheetAssignment assignment = new WorksheetAssignment();
            assignment.setWorksheetId(request.getWorksheetId());
            assignment.setTeacherId(request.getTeacherId());
            assignment.setStudentId(studentId);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDueDate(request.getDueDate());
            assignment.setCompleted(false);

            assignmentRepository.save(assignment);
        }
    }
}