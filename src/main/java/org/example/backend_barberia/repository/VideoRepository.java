package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.Video;
import org.example.backend_barberia.entity.VideoType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    
    List<Video> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    
    List<Video> findByCourseIdAndTypeOrderByOrderIndexAsc(Long courseId, VideoType type);
    
    List<Video> findByCourseIdAndActiveTrueOrderByOrderIndexAsc(Long courseId);
    
    int countByCourseId(Long courseId);
}
