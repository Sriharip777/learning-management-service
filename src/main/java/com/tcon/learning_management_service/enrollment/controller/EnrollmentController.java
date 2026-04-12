package com.tcon.learning_management_service.enrollment.controller;

import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.course.entity.CourseEnrollment;
import com.tcon.learning_management_service.course.repository.CourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/check")
    public ResponseEntity<Boolean> isStudentEnrolled(
            @RequestParam String studentId,
            @RequestParam String classId) {

        log.info("Enrollment check: studentId={} classId={}", studentId, classId);

        try {
            // ✅ Check 1: CourseEnrollment (group course)
            List<CourseEnrollment> courseEnrollments =
                    courseEnrollmentRepository.findByStudentId(studentId);

            boolean enrolledInCourse = courseEnrollments.stream()
                    .anyMatch(e ->
                            classId.equals(e.getCourseId()) &&
                                    e.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE
                    );

            if (enrolledInCourse) {
                log.info("✅ Student {} enrolled in course {} via CourseEnrollment",
                        studentId, classId);
                return ResponseEntity.ok(true);
            }

            // ✅ Check 2: Booking (one-on-one session)
            List<Booking> bookings = bookingRepository.findByStudentId(studentId);

            boolean hasBooking = bookings.stream()
                    .anyMatch(b ->
                            (classId.equals(b.getSessionId()) ||
                                    classId.equals(b.getId()) ||
                                    classId.equals(b.getTeacherId())) &&
                                    isActiveBookingStatus(b.getStatus())
                    );

            if (hasBooking) {
                log.info("✅ Student {} has booking for {} via Booking",
                        studentId, classId);
                return ResponseEntity.ok(true);
            }

            log.info("❌ Student {} NOT enrolled in class {} in any model",
                    studentId, classId);
            return ResponseEntity.ok(false);

        } catch (Exception e) {
            log.error("Enrollment check error: studentId={} classId={} error={}",
                    studentId, classId, e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    private boolean isActiveBookingStatus(BookingStatus status) {
        if (status == null) return false;
        return switch (status) {
            case CONFIRMED, COMPLETED -> true;
            default -> false;
        };
    }
}