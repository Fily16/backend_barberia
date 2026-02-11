package org.example.backend_barberia.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.response.ApiResponse;
import org.example.backend_barberia.dto.response.VideoResponse;
import org.example.backend_barberia.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;

    /**
     * Obtiene el video de portada de un curso (sin autenticación)
     */
    @GetMapping("/{slug}/cover")
    public ResponseEntity<ApiResponse<VideoResponse>> getCourseCover(@PathVariable String slug) {
        VideoResponse cover = courseService.getCoverVideo(slug);
        if (cover == null) {
            return ResponseEntity.ok(ApiResponse.error("No hay video de portada para este curso"));
        }
        return ResponseEntity.ok(ApiResponse.success(cover));
    }
}
