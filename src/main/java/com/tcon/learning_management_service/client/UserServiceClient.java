package com.tcon.learning_management_service.client;

import com.tcon.learning_management_service.client.dto.AssignedStudentOptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "auth-user-service",
        url = "${services.auth-user.url:http://localhost:8081}"
)
public interface UserServiceClient {

    @GetMapping("/api/student/teacher/{teacherId}/assigned-students")
    List<AssignedStudentOptionDto> getAssignedStudentsForTeacher(
            @PathVariable("teacherId") String teacherId
    );
}