package com.tcon.learning_management_service.course.controller;
import com.tcon.learning_management_service.course.dto.CourseCreateRequest;
import com.tcon.learning_management_service.course.dto.CourseDto;
import com.tcon.learning_management_service.course.dto.CourseSearchDto;
import com.tcon.learning_management_service.course.dto.CourseUpdateRequest;
import com.tcon.learning_management_service.course.entity.CourseEnrollment;
import com.tcon.learning_management_service.course.service.CourseEnrollmentService;
import com.tcon.learning_management_service.course.service.CourseSearchService;
import com.tcon.learning_management_service.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseSearchService courseSearchService;
    private final CourseEnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<CourseDto> createCourse(
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody CourseCreateRequest request) {
        log.info("Creating course for teacher: {}", teacherId);
        CourseDto course = courseService.createCourse(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto> getCourse(@PathVariable String courseId) {
        CourseDto course = courseService.getCourse(courseId);
        return ResponseEntity.ok(course);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<CourseDto>> getTeacherCourses(@PathVariable String teacherId) {
        List<CourseDto> courses = courseService.getTeacherCourses(teacherId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/published")
    public ResponseEntity<List<CourseDto>> getPublishedCourses() {
        List<CourseDto> courses = courseService.getPublishedCourses();
        return ResponseEntity.ok(courses);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseDto> updateCourse(
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody CourseUpdateRequest request) {
        CourseDto course = courseService.updateCourse(courseId, teacherId, request);
        return ResponseEntity.ok(course);
    }

    // ✅ CHANGE FROM @PostMapping TO @PatchMapping
    @PatchMapping("/{courseId}/publish")
    public ResponseEntity<Map<String, String>> publishCourse(
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String teacherId) {
        log.info("Publishing course: {} by teacher: {}", courseId, teacherId);
        courseService.publishCourse(courseId, teacherId);
        return ResponseEntity.ok(Map.of("message", "Course published successfully"));
    }

    // ✅ ADD THIS NEW ENDPOINT
    @PatchMapping("/{courseId}/unpublish")
    public ResponseEntity<Map<String, String>> unpublishCourse(
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String teacherId) {
        log.info("Unpublishing course: {} by teacher: {}", courseId, teacherId);
        courseService.unpublishCourse(courseId, teacherId);
        return ResponseEntity.ok(Map.of("message", "Course unpublished successfully"));
    }


    @DeleteMapping("/{courseId}")
    public ResponseEntity<Map<String, String>> deleteCourse(
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String teacherId) {
        courseService.deleteCourse(courseId, teacherId);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<List<CourseDto>> searchCourses(@RequestBody CourseSearchDto searchDto) {
        List<CourseDto> courses = courseSearchService.searchCourses(searchDto);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<CourseEnrollment> enrollStudent(
            @PathVariable String courseId,
            @RequestHeader("X-User-Id") String studentId,
            @RequestBody Map<String, Object> enrollmentData) {

        String studentName = (String) enrollmentData.get("studentName");
        String studentEmail = (String) enrollmentData.get("studentEmail");
        String paymentId = (String) enrollmentData.get("paymentId");
        Double amountPaid = ((Number) enrollmentData.get("amountPaid")).doubleValue();

        CourseEnrollment enrollment = enrollmentService.enrollStudent(
                courseId, studentId, studentName, studentEmail,
                paymentId, java.math.BigDecimal.valueOf(amountPaid)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @GetMapping("/{courseId}/enrollments")
    public ResponseEntity<List<CourseEnrollment>> getCourseEnrollments(@PathVariable String courseId) {
        List<CourseEnrollment> enrollments = enrollmentService.getCourseEnrollments(courseId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/student/{studentId}/enrollments")
    public ResponseEntity<List<CourseEnrollment>> getStudentEnrollments(@PathVariable String studentId) {
        List<CourseEnrollment> enrollments = enrollmentService.getStudentEnrollments(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/can-communicate")
    public ResponseEntity<Boolean> canUsersCommunicate(
            @RequestParam String user1,
            @RequestParam String user2) {
        log.info("Checking if {} can communicate with {}", user1, user2);
        boolean canCommunicate = courseService.canUsersCommunicate(user1, user2);
        return ResponseEntity.ok(canCommunicate);
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<Map<String, String>> cancelEnrollment(
            @PathVariable String enrollmentId,
            @RequestHeader("X-User-Id") String studentId) {
        enrollmentService.cancelEnrollment(enrollmentId, studentId);
        return ResponseEntity.ok(Map.of("message", "Enrollment cancelled successfully"));
    }

    // ✅ ADD THESE TO YOUR CourseController

    @GetMapping("/student/{studentId}/teachers")
    public ResponseEntity<List<String>> getTeachersForStudent(@PathVariable String studentId) {
        log.info("Getting teachers for student: {}", studentId);
        List<String> teacherIds = courseService.getTeachersForStudent(studentId);
        return ResponseEntity.ok(teacherIds);
    }

    @GetMapping("/teacher/{teacherId}/students")
    public ResponseEntity<List<String>> getStudentsForTeacher(@PathVariable String teacherId) {
        log.info("Getting students for teacher: {}", teacherId);
        List<String> studentIds = courseService.getStudentsForTeacher(teacherId);
        return ResponseEntity.ok(studentIds);
    }

}