package org.example.backend_barberia.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.response.ApiResponse;
import org.example.backend_barberia.dto.response.CourseResponse;
import org.example.backend_barberia.dto.response.UserCourseResponse;
import org.example.backend_barberia.dto.response.UserResponse;
import org.example.backend_barberia.security.CustomUserDetails;
import org.example.backend_barberia.service.CourseService;
import org.example.backend_barberia.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserService userService;
    private final CourseService courseService;

    /**
     * Obtiene el perfil del estudiante autenticado
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Obtiene los cursos asignados al estudiante
     */
    @GetMapping("/my-courses")
    public ResponseEntity<ApiResponse<List<UserCourseResponse>>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserCourseResponse> courses = courseService.getUserCourses(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    /**
     * Accede a un curso específico (verifica permisos y vigencia)
     */
    @GetMapping("/courses/{slug}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(
            @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseResponse course = courseService.getCourseForUser(slug, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(course));
    }
}
