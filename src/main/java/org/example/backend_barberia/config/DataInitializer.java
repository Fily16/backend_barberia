package org.example.backend_barberia.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.entity.*;
import org.example.backend_barberia.repository.CourseRepository;
import org.example.backend_barberia.repository.UserRepository;
import org.example.backend_barberia.repository.VideoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            CourseRepository courseRepository,
            VideoRepository videoRepository
    ) {
        return args -> {
            // Crear usuario ADMIN si no existe
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Alejandro (Ralph Cuts)")
                        .email("admin@ralphcuts.com")
                        .role(Role.ADMIN)
                        .active(true)
                        .build();
                userRepository.save(admin);
                log.info("✅ Usuario ADMIN creado: admin / admin123");
            }

            // Crear curso de ejemplo si no existe
            if (!courseRepository.existsBySlug("basic-training")) {
                Course course = Course.builder()
                        .slug("basic-training")
                        .title("BASIC TRAINING")
                        .description("Este curso es la introducción de entrenamiento básico que está diseñado para que aprendas el método utilizado por RaphlCuts con el cual ha generado resultados épicos!")
                        .thumbnailUrl("https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=800")
                        .orderIndex(0)
                        .active(true)
                        .build();
                courseRepository.save(course);

                // Videos de Teoría
                Video teoria = Video.builder()
                        .title("Fundamentos de la Barbería")
                        .description("Video teórico sobre los fundamentos básicos")
                        .videoUrl("https://www.youtube.com/embed/dQw4w9WgXcQ")
                        .thumbnailUrl("https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=600")
                        .duration("45:00")
                        .type(VideoType.THEORY)
                        .orderIndex(0)
                        .active(true)
                        .course(course)
                        .build();
                videoRepository.save(teoria);

                // Videos de Práctica
                String[] practicaTitles = {
                        "Técnicas Básicas",
                        "Manejo de Herramientas",
                        "Corte con Máquina",
                        "Acabados Profesionales"
                };
                String[] practicaDurations = {"15:30", "18:45", "20:00", "16:20"};

                for (int i = 0; i < practicaTitles.length; i++) {
                    Video practica = Video.builder()
                            .title(practicaTitles[i])
                            .videoUrl("https://www.youtube.com/embed/dQw4w9WgXcQ")
                            .thumbnailUrl("https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=300")
                            .duration(practicaDurations[i])
                            .type(VideoType.PRACTICE)
                            .orderIndex(i)
                            .active(true)
                            .course(course)
                            .build();
                    videoRepository.save(practica);
                }

                log.info("✅ Curso 'BASIC TRAINING' creado con 5 videos");
            }

            // Crear estudiante de prueba si no existe
            if (!userRepository.existsByUsername("prueba")) {
                User student = User.builder()
                        .username("prueba")
                        .password(passwordEncoder.encode("prueba123"))
                        .fullName("Estudiante de Prueba")
                        .email("prueba@test.com")
                        .role(Role.STUDENT)
                        .active(true)
                        .build();
                userRepository.save(student);
                log.info("✅ Estudiante de prueba creado: prueba / prueba123");
            }

            log.info("========================================");
            log.info("🚀 Ralph Cuts Academy API iniciada");
            log.info("📍 H2 Console: http://localhost:8080/h2-console");
            log.info("📍 API Base: http://localhost:8080/api");
            log.info("========================================");
        };
    }
}
