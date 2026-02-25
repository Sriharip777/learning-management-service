package com.tcon.learning_management_service.assignment.validation;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class AssignmentValidator {

    public void validateCreateRequest(
            AssignmentCreateRequest request)
    {

        if(request.getTitle()==null
                || request.getTitle().isEmpty())
        {
            throw new IllegalArgumentException(
                    "Assignment title required");
        }

        if(request.getTeacherId()==null
                || request.getTeacherId().isEmpty())
        {
            throw new IllegalArgumentException(
                    "TeacherId required");
        }

    }

}