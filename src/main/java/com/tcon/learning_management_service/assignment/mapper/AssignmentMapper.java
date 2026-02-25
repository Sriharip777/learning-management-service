package com.tcon.learning_management_service.assignment.mapper;

import com.tcon.learning_management_service.assignment.dto.AssignmentResponse;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {

    public AssignmentResponse map(Assignment assignment)
    {

        AssignmentResponse response =
                new AssignmentResponse();

        response.setId(assignment.getId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setTeacherId(assignment.getTeacherId());
        response.setDueDate(assignment.getDueDate());
        response.setStatus(assignment.getStatus());

        return response;
    }

}