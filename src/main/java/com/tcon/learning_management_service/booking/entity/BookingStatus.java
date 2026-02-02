package com.tcon.learning_management_service.booking.entity;

public enum BookingStatus {
    PENDING,           // Student sent request, waiting for teacher approval
    PENDING_PAYMENT,   // Teacher approved, waiting for student payment
    CONFIRMED,         // Payment received, booking confirmed
    REJECTED,          // Teacher rejected the request
    CANCELLED,
    COMPLETED,
    NO_SHOW,
    EXPIRED
}