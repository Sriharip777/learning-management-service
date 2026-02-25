package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsRequest {

    private List<String> studentIds;
}