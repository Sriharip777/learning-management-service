package com.tcon.learning_management_service.course.client;
import com.tcon.learning_management_service.course.dto.EligibleTeacherRequest;
import com.tcon.learning_management_service.course.dto.TeacherResponseDto;
import com.tcon.learning_management_service.course.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        configuration = com.tcon.learning_management_service.config.FeignClientConfiguration.class
)
public interface UserServiceClient {

    @GetMapping("/api/teachers/{teacherId}")
    TeacherResponseDto getTeacherById(@PathVariable("teacherId") String teacherId);

    @GetMapping("/api/teacher/profile/{userId}")
    TeacherResponseDto getTeacherByUserId(@PathVariable("userId") String userId);

    @PostMapping("/api/teacher/eligible-for-course")
    List<TeacherResponseDto> getEligibleTeachersForCourse(
            @RequestBody EligibleTeacherRequest request);

    @GetMapping("/api/users/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") String userId);
}