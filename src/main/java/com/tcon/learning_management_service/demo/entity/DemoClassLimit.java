package com.tcon.learning_management_service.demo.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "demo_class_limits")
public class DemoClassLimit {

    @Id
    private String id;

    @Indexed(unique = true)
    private String studentId;

    @Builder.Default
    private Integer totalDemosAllowed = 3;

    @Builder.Default
    private Integer demosUsed = 0;

    private LocalDateTime firstDemoAt;
    private LocalDateTime lastDemoAt;

    private LocalDateTime resetAt;

    @Builder.Default
    private Boolean isLimitActive = true;
}
