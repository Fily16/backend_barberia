package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    Optional<Course> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    List<Course> findByActiveTrueOrderByOrderIndexAsc();
    
    List<Course> findAllByOrderByOrderIndexAsc();
}
