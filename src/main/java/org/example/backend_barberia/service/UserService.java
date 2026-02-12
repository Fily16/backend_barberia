package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.dto.request.CreateUserRequest;
import org.example.backend_barberia.dto.response.UserCourseResponse;
import org.example.backend_barberia.dto.response.UserResponse;
import org.example.backend_barberia.entity.Role;
import org.example.backend_barberia.entity.User;
import org.example.backend_barberia.entity.UserCourse;
import org.example.backend_barberia.exception.BadRequestException;
import org.example.backend_barberia.exception.ResourceNotFoundException;
import org.example.backend_barberia.repository.UserRepository;
import org.example.backend_barberia.repository.PaymentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<UserResponse> getAllStudents() {
        return userRepository.findByRole(Role.STUDENT)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        return mapToResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse createStudent(CreateUserRequest request) {
        // Verificar si el username ya existe
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        // Normalizar email: tratar vacío como null
        String email = request.getEmail();
        if (email != null && email.isBlank()) {
            email = null;
            request.setEmail(null);
        }

        // Verificar email si se proporciona (solo entre STUDENTs, no incluir ADMIN)
        if (email != null) {
            var existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                // Solo bloquear si el usuario con ese email es STUDENT activo
                User existing = existingUser.get();
                if (existing.getRole() == Role.STUDENT) {
                    throw new BadRequestException("El email ya está registrado por el estudiante: " + existing.getFullName());
                }
            }
        }

        // Guardar password original antes de encriptar (para enviar por correo)
        String originalPassword = request.getPassword();

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(Role.STUDENT)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        
        // Enviar correo de bienvenida si tiene email
        if (savedUser.getEmail() != null && !savedUser.getEmail().isEmpty()) {
            try {
                boolean emailSent = emailService.sendWelcomeEmail(
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        savedUser.getUsername(),
                        originalPassword
                );
                if (emailSent) {
                    log.info("Correo de bienvenida enviado a: {}", savedUser.getEmail());
                }
            } catch (Exception e) {
                // No fallar si el correo no se envia
                log.error("Error enviando correo de bienvenida: {}", e.getMessage());
            }
        }
        
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateStudent(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        // Verificar username si cambió
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        // Normalizar email: tratar vacío como null
        String email = request.getEmail();
        if (email != null && email.isBlank()) {
            email = null;
        }

        // Verificar email si cambió y no es null
        if (email != null && !email.equals(user.getEmail())) {
            var existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                throw new BadRequestException("El email ya está registrado por: " + existingUser.get().getFullName());
            }
        }

        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPhone(request.getPhone());

        // Solo actualizar password si se proporciona uno nuevo
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        user.setActive(!user.getActive());
        
        // Si se desactiva, también invalidar su sesión
        if (!user.getActive()) {
            user.setCurrentSessionToken(null);
        }
        
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        // Eliminar pagos relacionados primero
        paymentRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        user.setPassword(passwordEncoder.encode(newPassword));
        // Invalidar sesión actual para forzar re-login
        user.setCurrentSessionToken(null);
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        List<UserCourseResponse> courses = user.getUserCourses()
                .stream()
                .map(this::mapUserCourseToResponse)
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .courses(courses)
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
