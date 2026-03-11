package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.dto.CourseDto;
import com.tcon.learning_management_service.course.dto.CourseSearchDto;
import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final MongoTemplate mongoTemplate;

    public List<CourseDto> searchCourses(CourseSearchDto searchDto) {
        log.info("Searching courses with criteria: {}", searchDto);

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Keyword search (title or description)
        if (searchDto.getKeyword() != null && !searchDto.getKeyword().isEmpty()) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(searchDto.getKeyword(), "i"),
                    Criteria.where("description").regex(searchDto.getKeyword(), "i")
            );
            criteriaList.add(keywordCriteria);
        }

        // Grade filter (replaces old category)
        if (searchDto.getGradeId() != null && !searchDto.getGradeId().isEmpty()) {
            criteriaList.add(Criteria.where("gradeId").is(searchDto.getGradeId()));
        }

        // Subject filter
        if (searchDto.getSubjectId() != null && !searchDto.getSubjectId().isEmpty()) {
            criteriaList.add(Criteria.where("subjectId").is(searchDto.getSubjectId()));
        }

        // Topic filter
        if (searchDto.getTopicIds() != null && !searchDto.getTopicIds().isEmpty()) {
            criteriaList.add(Criteria.where("topicIds").in(searchDto.getTopicIds()));
        }

        // Teacher filter
        if (searchDto.getTeacherId() != null && !searchDto.getTeacherId().isEmpty()) {
            criteriaList.add(Criteria.where("teacherId").is(searchDto.getTeacherId()));
        }

        // Status filter
        if (searchDto.getStatuses() != null && !searchDto.getStatuses().isEmpty()) {
            criteriaList.add(Criteria.where("status").in(searchDto.getStatuses()));
        } else {
            // Default: only show published courses
            criteriaList.add(Criteria.where("status").is(CourseStatus.PUBLISHED));
        }

        // Grade level filter
        if (searchDto.getGradeLevel() != null && !searchDto.getGradeLevel().isEmpty()) {
            criteriaList.add(Criteria.where("gradeLevel").is(searchDto.getGradeLevel()));
        }

        // Difficulty filter
        if (searchDto.getDifficulty() != null && !searchDto.getDifficulty().isEmpty()) {
            criteriaList.add(Criteria.where("difficulty").is(searchDto.getDifficulty()));
        }

        // Price range filter
        if (searchDto.getMinPrice() != null) {
            criteriaList.add(Criteria.where("pricePerSession").gte(searchDto.getMinPrice()));
        }
        if (searchDto.getMaxPrice() != null) {
            criteriaList.add(Criteria.where("pricePerSession").lte(searchDto.getMaxPrice()));
        }

        // Rating filter
        if (searchDto.getMinRating() != null) {
            criteriaList.add(Criteria.where("rating").gte(searchDto.getMinRating()));
        }

        // Demo availability filter
        if (searchDto.getIsDemoAvailable() != null) {
            criteriaList.add(Criteria.where("isDemoAvailable").is(searchDto.getIsDemoAvailable()));
        }

        // Combine all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Sorting
        Sort.Direction direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        query.with(Sort.by(direction, searchDto.getSortBy()));

        // Pagination
        Pageable pageable = PageRequest.of(searchDto.getPage(), searchDto.getSize());
        query.with(pageable);

        List<Course> courses = mongoTemplate.find(query, Course.class);
        log.info("Found {} courses matching search criteria", courses.size());

        return courses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CourseDto toDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .title(course.getTitle())
                .description(course.getDescription())
                .gradeId(course.getGradeId())
                .subjectId(course.getSubjectId())
                .topicIds(course.getTopicIds())
                .status(course.getStatus())
                .pricePerSession(course.getPricePerSession())
                .currency(course.getCurrency())
                .minStudents(course.getMinStudents())
                .maxStudents(course.getMaxStudents())
                .currentEnrollments(course.getCurrentEnrollments())
                .gradeLevel(course.getGradeLevel())
                .difficulty(course.getDifficulty())
                .schedule(course.getSchedule())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .totalSessions(course.getTotalSessions())
                .completedSessions(course.getCompletedSessions())
                .prerequisites(course.getPrerequisites())
                .learningOutcomes(course.getLearningOutcomes())
                .thumbnailUrl(course.getThumbnailUrl())
                .materialUrls(course.getMaterialUrls())
                .isDemoAvailable(course.getIsDemoAvailable())
                .demoSessionDuration(course.getDemoSessionDuration())
                .rating(course.getRating())
                .totalReviews(course.getTotalReviews())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
