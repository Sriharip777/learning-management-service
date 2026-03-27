package com.tcon.learning_management_service.worksheet.integration;

import com.tcon.learning_management_service.worksheet.integration.dto.SubjectDto;
import com.tcon.learning_management_service.worksheet.integration.dto.TopicDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseIntegrationService {

    private final CourseClient courseClient;

    /*
     * Validate academic hierarchy and return topic
     */
    public TopicDto validateSubjectAndGrade(
            String subjectId,
            String gradeId,
            String topicId
    ) {

        log.info(
                "Validating subject={}, grade={}, topic={}",
                subjectId,
                gradeId,
                topicId
        );

        try {

            // ✅ 1. Validate subject belongs to grade
            List<SubjectDto> subjects =
                    courseClient.getSubjectsByGrade(gradeId);

            boolean subjectValid = subjects.stream()
                    .anyMatch(s -> s.getId().equals(subjectId));

            if (!subjectValid) {
                throw new RuntimeException("Invalid subject for selected grade");
            }

            // ✅ 2. Fetch topics for subject
            List<TopicDto> topics =
                    courseClient.getTopicsBySubject(subjectId);

            // ✅ 3. Find topic
            TopicDto topic = topics.stream()
                    .filter(t -> t.getId().equals(topicId))
                    .findFirst()
                    .orElseThrow(() ->
                            new RuntimeException("Invalid topic for selected subject"));

            log.info("Validation successful for topic={}", topic.getName());

            return topic; // 🔥 IMPORTANT (for duration)

        } catch (Exception e) {
            log.error("Error validating course hierarchy", e);
            throw new RuntimeException("Failed to validate course data");
        }
    }
}