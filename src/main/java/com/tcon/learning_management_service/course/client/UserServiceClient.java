package com.tcon.learning_management_service.course.client;


import com.tcon.learning_management_service.course.dto.TeacherResponseDto;
import com.tcon.learning_management_service.course.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/teachers/{teacherId}")
    TeacherResponseDto getTeacherById(@PathVariable("teacherId") String teacherId);

    @GetMapping("/api/users/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") String userId);
}
