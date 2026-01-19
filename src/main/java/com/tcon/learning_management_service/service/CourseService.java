package com.tcon.learning_management_service.service;

import com.tcon.learning_management_service.dto.CourseCreateRequest;
import com.tcon.learning_management_service.dto.CourseDto;
import com.tcon.learning_management_service.entity.Course;
import com.tcon.learning_management_service.entity.CourseStatus;
import com.tcon.learning_management_service.event.CourseEventPublisher;
import com.tcon.learning_management_service.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseEventPublisher courseEventPublisher;

    public CourseDto createCourse(CourseCreateRequest request) {
        log.info("Creating course: {}", request.getTitle());

        Course course = Course.builder()
                .teacherId(request.getTeacherId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(CourseStatus.ACTIVE)
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .isRecurring(request.getIsRecurring())
                .schedule(request.getSchedule())
                .tags(request.getTags())
                .level(request.getLevel())
                .maxStudents(request.getMaxStudents())
                .enrolledStudents(0)
                .build();

        Course savedCourse = courseRepository.save(course);

        // Publish course created event
        courseEventPublisher.publishCourseCreated(savedCourse);

        return convertToDto(savedCourse);
    }

    public CourseDto getCourseById(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        return convertToDto(course);
    }

    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<CourseDto> getCoursesByTeacher(String teacherId) {
        return courseRepository.findByTeacherId(teacherId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<CourseDto> getActiveCourses() {
        return courseRepository.findByStatus(CourseStatus.ACTIVE)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public CourseDto updateCourse(String id, CourseCreateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setDurationMinutes(request.getDurationMinutes());
        course.setPrice(request.getPrice());
        course.setCurrency(request.getCurrency());
        course.setIsRecurring(request.getIsRecurring());
        course.setSchedule(request.getSchedule());
        course.setTags(request.getTags());
        course.setLevel(request.getLevel());
        course.setMaxStudents(request.getMaxStudents());

        Course updatedCourse = courseRepository.save(course);
        return convertToDto(updatedCourse);
    }

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
        log.info("Course deleted: {}", id);
    }

    private CourseDto convertToDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .status(course.getStatus())
                .durationMinutes(course.getDurationMinutes())
                .price(course.getPrice())
                .currency(course.getCurrency())
                .isRecurring(course.getIsRecurring())
                .schedule(course.getSchedule())
                .tags(course.getTags())
                .level(course.getLevel())
                .maxStudents(course.getMaxStudents())
                .enrolledStudents(course.getEnrolledStudents())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
