package org.example.backend_barberia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.request.AssignCourseRequest;
import org.example.backend_barberia.dto.request.CreateCourseRequest;
import org.example.backend_barberia.dto.request.CreateUserRequest;
import org.example.backend_barberia.dto.request.CreateVideoRequest;
import org.example.backend_barberia.dto.request.ExtendAccessRequest;
import org.example.backend_barberia.dto.response.*;
import org.example.backend_barberia.service.CourseService;
import org.example.backend_barberia.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;

    // ==================== ESTUDIANTES ====================

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllStudents() {
        List<UserResponse> students = userService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getStudent(@PathVariable Long id) {
        UserResponse student = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(student));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<UserResponse>> createStudent(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse student = userService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Estudiante creado exitosamente", student));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse student = userService.updateStudent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Estudiante actualizado", student));
    }

    @PatchMapping("/students/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStudentStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado", null));
    }

    @PatchMapping("/students/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada", null));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Estudiante eliminado", null));
    }

    // ==================== CURSOS ====================

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        List<CourseResponse> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success(course));
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse course = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Curso creado exitosamente", course));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse course = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success("Curso actualizado", course));
    }

    @PatchMapping("/courses/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleCourseStatus(@PathVariable Long id) {
        courseService.toggleCourseStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Estado del curso actualizado", null));
    }

    // ==================== VIDEOS ====================

    @PostMapping("/videos")
    public ResponseEntity<ApiResponse<VideoResponse>> createVideo(
            @Valid @RequestBody CreateVideoRequest request) {
        VideoResponse video = courseService.createVideo(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Video creado exitosamente", video));
    }

    @PutMapping("/videos/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody CreateVideoRequest request) {
        VideoResponse video = courseService.updateVideo(id, request);
        return ResponseEntity.ok(ApiResponse.success("Video actualizado", video));
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable Long id) {
        courseService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success("Video eliminado", null));
    }

    // ==================== ASIGNACIÓN DE CURSOS ====================

    @PostMapping("/assign-course")
    public ResponseEntity<ApiResponse<UserCourseResponse>> assignCourse(
            @Valid @RequestBody AssignCourseRequest request) {
        UserCourseResponse assignment = courseService.assignCourseToUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Curso asignado exitosamente", assignment));
    }

    @GetMapping("/students/{userId}/courses")
    public ResponseEntity<ApiResponse<List<UserCourseResponse>>> getStudentCourses(
            @PathVariable Long userId) {
        List<UserCourseResponse> courses = courseService.getUserCourses(userId);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @PatchMapping("/user-courses/{id}/extend")
    public ResponseEntity<ApiResponse<UserCourseResponse>> extendAccess(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        Integer months = request.get("months");
        UserCourseResponse assignment = courseService.extendAccess(id, months);
        return ResponseEntity.ok(ApiResponse.success("Acceso extendido", assignment));
    }
    
    /**
     * Extiende el acceso con registro de pago (renovación)
     * POST /api/admin/user-courses/extend-with-payment
     */
    @PostMapping("/user-courses/extend-with-payment")
    public ResponseEntity<ApiResponse<UserCourseResponse>> extendAccessWithPayment(
            @Valid @RequestBody ExtendAccessRequest request) {
        UserCourseResponse assignment = courseService.extendAccessWithPayment(
                request.getUserCourseId(),
                request.getDurationDays(),
                request.getDurationMonths(),
                request.getAmount(),
                request.getCurrency()
        );
        return ResponseEntity.ok(ApiResponse.success("Acceso extendido y pago registrado", assignment));
    }

    @PatchMapping("/user-courses/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeAccess(@PathVariable Long id) {
        courseService.revokeAccess(id);
        return ResponseEntity.ok(ApiResponse.success("Acceso revocado", null));
    }
}
