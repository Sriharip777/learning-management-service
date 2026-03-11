package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.dto.GradeDto;
import com.tcon.learning_management_service.course.entity.Grade;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    public GradeDto create(GradeDto dto) {
        Grade grade = Grade.builder()
                .name(dto.getName())
                .order(dto.getOrder() != null ? dto.getOrder() : 0)
                .isActive(true)
                .build();
        return toDto(gradeRepository.save(grade));
    }

    public List<GradeDto> getAll() {
        return gradeRepository.findByIsActiveTrueOrderByOrderAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public GradeDto update(String id, GradeDto dto) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + id));
        if (dto.getName() != null) grade.setName(dto.getName());
        if (dto.getOrder() != null) grade.setOrder(dto.getOrder());
        if (dto.getIsActive() != null) grade.setIsActive(dto.getIsActive());
        return toDto(gradeRepository.save(grade));
    }

    public void delete(String id) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + id));
        grade.setIsActive(false);
        gradeRepository.save(grade);
    }

    private GradeDto toDto(Grade g) {
        return GradeDto.builder()
                .id(g.getId())
                .name(g.getName())
                .order(g.getOrder())
                .isActive(g.getIsActive())
                .build();
    }
}
