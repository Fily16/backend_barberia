package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    
    List<UserCourse> findByUserId(Long userId);
    
    List<UserCourse> findByUserIdAndActiveTrue(Long userId);
    
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    
    Optional<UserCourse> findByUserIdAndCourseSlug(Long userId, String courseSlug);
    
    @Query("SELECT uc FROM UserCourse uc WHERE uc.user.id = :userId AND uc.course.slug = :slug AND uc.active = true")
    Optional<UserCourse> findActiveByUserIdAndCourseSlug(@Param("userId") Long userId, @Param("slug") String slug);
    
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    
    List<UserCourse> findByCourseId(Long courseId);
}
