package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.dto.TopicDto;
import com.tcon.learning_management_service.course.entity.Topic;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import com.tcon.learning_management_service.course.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    public TopicDto create(TopicDto dto) {
        subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + dto.getSubjectId()));

        Topic topic = Topic.builder()
                .subjectId(dto.getSubjectId())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        return toDto(topicRepository.save(topic));
    }

    public List<TopicDto> getBySubject(String subjectId) {
        return topicRepository.findBySubjectIdAndIsActiveTrue(subjectId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public TopicDto update(String id, TopicDto dto) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
        if (dto.getName() != null) topic.setName(dto.getName());
        if (dto.getDescription() != null) topic.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) topic.setIsActive(dto.getIsActive());
        return toDto(topicRepository.save(topic));
    }

    public void delete(String id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
        topic.setIsActive(false);
        topicRepository.save(topic);
    }

    private TopicDto toDto(Topic t) {
        return TopicDto.builder()
                .id(t.getId())
                .subjectId(t.getSubjectId())
                .name(t.getName())
                .description(t.getDescription())
                .isActive(t.getIsActive())
                .build();
    }
}
