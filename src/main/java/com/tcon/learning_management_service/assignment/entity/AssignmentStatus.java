package com.tcon.learning_management_service.assignment.entity;

public enum AssignmentStatus {

    CREATED,   // Assignment created but not assigned
    ACTIVE,    // Students assigned, accepting submissions
    CLOSED     // Due date passed / manually closed
}