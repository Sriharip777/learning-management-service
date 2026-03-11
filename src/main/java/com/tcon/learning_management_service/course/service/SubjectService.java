package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.dto.SubjectDto;
import com.tcon.learning_management_service.course.entity.Subject;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    public SubjectDto create(SubjectDto dto) {
        gradeRepository.findById(dto.getGradeId())
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + dto.getGradeId()));

        Subject subject = Subject.builder()
                .gradeId(dto.getGradeId())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        return toDto(subjectRepository.save(subject));
    }

    public List<SubjectDto> getByGrade(String gradeId) {
        return subjectRepository.findByGradeIdAndIsActiveTrue(gradeId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SubjectDto update(String id, SubjectDto dto) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        if (dto.getName() != null) subject.setName(dto.getName());
        if (dto.getDescription() != null) subject.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) subject.setIsActive(dto.getIsActive());
        return toDto(subjectRepository.save(subject));
    }

    public void delete(String id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        subject.setIsActive(false);
        subjectRepository.save(subject);
    }

    private SubjectDto toDto(Subject s) {
        return SubjectDto.builder()
                .id(s.getId())
                .gradeId(s.getGradeId())
                .name(s.getName())
                .description(s.getDescription())
                .isActive(s.getIsActive())
                .build();
    }
}
