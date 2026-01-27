package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.request.AssignCourseRequest;
import org.example.backend_barberia.dto.request.CreateCourseRequest;
import org.example.backend_barberia.dto.request.CreateVideoRequest;
import org.example.backend_barberia.dto.response.CourseResponse;
import org.example.backend_barberia.dto.response.UserCourseResponse;
import org.example.backend_barberia.dto.response.VideoResponse;
import org.example.backend_barberia.entity.*;
import org.example.backend_barberia.exception.AccessDeniedException;
import org.example.backend_barberia.exception.BadRequestException;
import org.example.backend_barberia.exception.ResourceNotFoundException;
import org.example.backend_barberia.repository.CourseRepository;
import org.example.backend_barberia.repository.UserCourseRepository;
import org.example.backend_barberia.repository.UserRepository;
import org.example.backend_barberia.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;

    // ==================== CURSOS ====================

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findByActiveTrueOrderByOrderIndexAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + slug));
        return mapToResponse(course);
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", id));
        return mapToResponse(course);
    }

    /**
     * Obtiene un curso verificando que el usuario tenga acceso válido
     */
    public CourseResponse getCourseForUser(String slug, Long userId) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado: " + slug));

        // Verificar acceso del usuario
        UserCourse userCourse = userCourseRepository.findActiveByUserIdAndCourseSlug(userId, slug)
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a este curso"));

        if (!userCourse.isAccessValid()) {
            throw new AccessDeniedException("Tu acceso a este curso ha expirado");
        }

        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Ya existe un curso con ese slug");
        }

        Course course = Course.builder()
                .slug(request.getSlug())
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .orderIndex(request.getOrderIndex())
                .active(true)
                .build();

        Course savedCourse = courseRepository.save(course);
        return mapToResponse(savedCourse);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CreateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", id));

        // Verificar slug si cambió
        if (!course.getSlug().equals(request.getSlug()) && 
            courseRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Ya existe un curso con ese slug");
        }

        course.setSlug(request.getSlug());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setOrderIndex(request.getOrderIndex());

        Course savedCourse = courseRepository.save(course);
        return mapToResponse(savedCourse);
    }

    @Transactional
    public void toggleCourseStatus(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", id));
        course.setActive(!course.getActive());
        courseRepository.save(course);
    }

    // ==================== VIDEOS ====================

    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso", request.getCourseId()));

        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .duration(request.getDuration())
                .type(request.getType())
                .orderIndex(request.getOrderIndex())
                .active(true)
                .course(course)
                .build();

        Video savedVideo = videoRepository.save(video);
        return mapVideoToResponse(savedVideo);
    }

    @Transactional
    public VideoResponse updateVideo(Long id, CreateVideoRequest request) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", id));

        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setVideoUrl(request.getVideoUrl());
        video.setThumbnailUrl(request.getThumbnailUrl());
        video.setDuration(request.getDuration());
        video.setType(request.getType());
        video.setOrderIndex(request.getOrderIndex());

        Video savedVideo = videoRepository.save(video);
        return mapVideoToResponse(savedVideo);
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", id));
        videoRepository.delete(video);
    }

    // ==================== ASIGNACIÓN DE CURSOS ====================

    @Transactional
    public UserCourseResponse assignCourseToUser(AssignCourseRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.getUserId()));
        
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso", request.getCourseId()));

        // Verificar si ya tiene asignación activa
        if (userCourseRepository.existsByUserIdAndCourseId(request.getUserId(), request.getCourseId())) {
            throw new BadRequestException("El usuario ya tiene este curso asignado");
        }

        LocalDateTime expiresAt = null;
        if (request.getPlanType() == PlanType.TEMPORAL && request.getDurationMonths() != null) {
            expiresAt = LocalDateTime.now().plusMonths(request.getDurationMonths());
        }

        UserCourse userCourse = UserCourse.builder()
                .user(user)
                .course(course)
                .planType(request.getPlanType())
                .expiresAt(expiresAt)
                .active(true)
                .build();

        UserCourse saved = userCourseRepository.save(userCourse);
        return mapUserCourseToResponse(saved);
    }

    @Transactional
    public UserCourseResponse extendAccess(Long userCourseId, Integer additionalMonths) {
        UserCourse userCourse = userCourseRepository.findById(userCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación", userCourseId));

        if (userCourse.getPlanType() == PlanType.UNLIMITED) {
            throw new BadRequestException("No se puede extender un plan ilimitado");
        }

        LocalDateTime newExpiry;
        if (userCourse.getExpiresAt() != null && userCourse.getExpiresAt().isAfter(LocalDateTime.now())) {
            // Extender desde la fecha actual de expiración
            newExpiry = userCourse.getExpiresAt().plusMonths(additionalMonths);
        } else {
            // Extender desde ahora
            newExpiry = LocalDateTime.now().plusMonths(additionalMonths);
        }

        userCourse.setExpiresAt(newExpiry);
        userCourse.setActive(true);
        
        UserCourse saved = userCourseRepository.save(userCourse);
        return mapUserCourseToResponse(saved);
    }

    @Transactional
    public void revokeAccess(Long userCourseId) {
        UserCourse userCourse = userCourseRepository.findById(userCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación", userCourseId));
        userCourse.setActive(false);
        userCourseRepository.save(userCourse);
    }

    public List<UserCourseResponse> getUserCourses(Long userId) {
        return userCourseRepository.findByUserId(userId)
                .stream()
                .map(this::mapUserCourseToResponse)
                .collect(Collectors.toList());
    }

    // ==================== MAPPERS ====================

    private CourseResponse mapToResponse(Course course) {
        List<VideoResponse> theoryVideos = course.getVideos().stream()
                .filter(v -> v.getType() == VideoType.THEORY && v.getActive())
                .map(this::mapVideoToResponse)
                .collect(Collectors.toList());

        List<VideoResponse> practiceVideos = course.getVideos().stream()
                .filter(v -> v.getType() == VideoType.PRACTICE && v.getActive())
                .map(this::mapVideoToResponse)
                .collect(Collectors.toList());

        return CourseResponse.builder()
                .id(course.getId())
                .slug(course.getSlug())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .active(course.getActive())
                .orderIndex(course.getOrderIndex())
                .totalVideos(theoryVideos.size() + practiceVideos.size())
                .theoryVideos(theoryVideos)
                .practiceVideos(practiceVideos)
                .build();
    }

    private VideoResponse mapVideoToResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .type(video.getType())
                .orderIndex(video.getOrderIndex())
                .build();
    }

    private UserCourseResponse mapUserCourseToResponse(UserCourse uc) {
        return UserCourseResponse.builder()
                .id(uc.getId())
                .courseId(uc.getCourse().getId())
                .courseSlug(uc.getCourse().getSlug())
                .courseTitle(uc.getCourse().getTitle())
                .planType(uc.getPlanType())
                .expiresAt(uc.getExpiresAt())
                .active(uc.getActive())
                .accessValid(uc.isAccessValid())
                .assignedAt(uc.getAssignedAt())
                .build();
    }
}
