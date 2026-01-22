package com.tcon.learning_management_service.demo.repository;


import com.tcon.learning_management_service.demo.entity.DemoClassLimit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemoClassLimitRepository extends MongoRepository<DemoClassLimit, String> {

    Optional<DemoClassLimit> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
