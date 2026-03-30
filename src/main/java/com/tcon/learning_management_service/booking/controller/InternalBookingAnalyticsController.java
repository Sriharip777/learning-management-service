package com.tcon.learning_management_service.booking.controller;

import com.tcon.learning_management_service.booking.dto.MonthlyClassStatDto;
import com.tcon.learning_management_service.booking.dto.StudentBookingAnalyticsDto;
import com.tcon.learning_management_service.booking.dto.TeacherBookingAnalyticsDto;
import com.tcon.learning_management_service.booking.service.InternalBookingAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/analytics")
@RequiredArgsConstructor
public class InternalBookingAnalyticsController {

    private final InternalBookingAnalyticsService analyticsService;

    @GetMapping("/teachers")
    public List<TeacherBookingAnalyticsDto> getTeacherAnalytics() {
        return analyticsService.getTeacherAnalytics();
    }

    @GetMapping("/students")
    public List<StudentBookingAnalyticsDto> getStudentAnalytics() {
        return analyticsService.getStudentAnalytics();
    }

    @GetMapping("/overview")
    public List<MonthlyClassStatDto> getOverviewStats() {
        return analyticsService.getOverviewStats();
    }
}