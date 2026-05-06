package com.tcon.learning_management_service.client;


import com.tcon.learning_management_service.client.dto.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "content-service")
public interface CourseClient {

    @GetMapping("/api/courses/{courseId}")
    CourseDto getCourseById(@PathVariable("courseId") String courseId);
}