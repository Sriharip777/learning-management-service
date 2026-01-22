package com.tcon.learning_management_service.booking.dto;



import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancellationRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
