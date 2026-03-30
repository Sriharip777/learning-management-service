package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.booking.dto.MonthlyClassStatDto;
import com.tcon.learning_management_service.booking.dto.StudentBookingAnalyticsDto;
import com.tcon.learning_management_service.booking.dto.TeacherBookingAnalyticsDto;
import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalBookingAnalyticsService {

    private final BookingRepository bookingRepository;

    public List<TeacherBookingAnalyticsDto> getTeacherAnalytics() {
        List<Booking> bookings = bookingRepository.findAll();

        return bookings.stream()
                .filter(b -> b.getTeacherId() != null)
                .collect(Collectors.groupingBy(Booking::getTeacherId))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Booking> teacherBookings = entry.getValue();

                    int total = teacherBookings.size();
                    int completed = (int) teacherBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                            .count();
                    int cancelled = (int) teacherBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                            .count();
                    int uniqueStudents = (int) teacherBookings.stream()
                            .map(Booking::getStudentId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .count();

                    return TeacherBookingAnalyticsDto.builder()
                            .teacherId(entry.getKey())
                            .totalClasses(total)
                            .completedClasses(completed)
                            .cancelledClasses(cancelled)
                            .uniqueStudents(uniqueStudents)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<StudentBookingAnalyticsDto> getStudentAnalytics() {
        List<Booking> bookings = bookingRepository.findAll();

        return bookings.stream()
                .filter(b -> b.getStudentId() != null)
                .collect(Collectors.groupingBy(Booking::getStudentId))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Booking> studentBookings = entry.getValue();

                    int total = studentBookings.size();
                    int completed = (int) studentBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                            .count();
                    int cancelled = (int) studentBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                            .count();

                    int totalMinutes = studentBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                            .mapToInt(b -> {
                                if (b.getDurationMinutes() != null) {
                                    return b.getDurationMinutes();
                                }
                                if (b.getSessionStartTime() != null && b.getSessionEndTime() != null) {
                                    return (int) Duration.between(
                                            b.getSessionStartTime(),
                                            b.getSessionEndTime()
                                    ).toMinutes();
                                }
                                return 0;
                            })
                            .sum();

                    return StudentBookingAnalyticsDto.builder()
                            .studentId(entry.getKey())
                            .totalClasses(total)
                            .completedClasses(completed)
                            .cancelledClasses(cancelled)
                            .totalMinutesLearned(totalMinutes)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<MonthlyClassStatDto> getOverviewStats() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<String, Integer> grouped = new LinkedHashMap<>();

        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            grouped.put(monthLabel(ym.getMonthValue()), 0);
        }

        for (Booking booking : bookings) {
            if (booking.getSessionStartTime() != null) {
                String label = monthLabel(booking.getSessionStartTime().getMonthValue());
                if (grouped.containsKey(label)) {
                    grouped.put(label, grouped.get(label) + 1);
                }
            }
        }

        return grouped.entrySet().stream()
                .map(e -> MonthlyClassStatDto.builder()
                        .label(e.getKey())
                        .classes(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String monthLabel(int month) {
        return switch (month) {
            case 1 -> "Jan";
            case 2 -> "Feb";
            case 3 -> "Mar";
            case 4 -> "Apr";
            case 5 -> "May";
            case 6 -> "Jun";
            case 7 -> "Jul";
            case 8 -> "Aug";
            case 9 -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Dec";
            default -> "N/A";
        };
    }
}