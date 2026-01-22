package com.tcon.learning_management_service.booking.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicy {
    private Integer hoursBeforeSession;
    private Integer refundPercentage;
    private String policyDescription;
}
