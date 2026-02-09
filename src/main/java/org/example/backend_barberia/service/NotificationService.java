package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.entity.NotificationSettings;
import org.example.backend_barberia.entity.User;
import org.example.backend_barberia.entity.UserCourse;
import org.example.backend_barberia.repository.NotificationSettingsRepository;
import org.example.backend_barberia.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Obtiene la configuracion de notificaciones
     */
    public NotificationSettings getSettings() {
        return settingsRepository.getOrCreate();
    }

    /**
     * Guarda la configuracion de notificaciones
     */
    public NotificationSettings saveSettings(NotificationSettings settings) {
        settings.setId(1L);
        return settingsRepository.save(settings);
    }

    /**
     * Genera link de WhatsApp para un usuario
     */
    public String generateWhatsAppLink(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        
        // Limpiar numero (quitar espacios, guiones, etc)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // Si no tiene codigo de pais, agregar Peru (51)
        if (cleanNumber.length() == 9) {
            cleanNumber = "51" + cleanNumber;
        }
        
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + cleanNumber + "?text=" + encodedMessage;
    }

    /**
     * Genera links de WhatsApp de bienvenida para un usuario
     */
    public String generateWelcomeWhatsAppLink(User user, String username, String password) {
        NotificationSettings settings = getSettings();
        
        String message = settings.getWhatsappWelcomeTemplate()
                .replace("{nombre}", user.getFullName())
                .replace("{usuario}", username)
                .replace("{password}", password)
                .replace("{url}", "https://ralph-cuts-academy.vercel.app/curso");
        
        return generateWhatsAppLink(user.getPhone(), message);
    }

    /**
     * Genera links de WhatsApp para todos los usuarios o seleccionados
     */
    public List<WhatsAppLinkDto> generateMassiveWhatsAppLinks(List<Long> userIds, String customMessage) {
        NotificationSettings settings = getSettings();
        List<User> users;
        
        if (userIds == null || userIds.isEmpty()) {
            // Todos los estudiantes con telefono
            users = userRepository.findAll().stream()
                    .filter(u -> u.getPhone() != null && !u.getPhone().isEmpty())
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAllById(userIds).stream()
                    .filter(u -> u.getPhone() != null && !u.getPhone().isEmpty())
                    .collect(Collectors.toList());
        }

        List<WhatsAppLinkDto> links = new ArrayList<>();
        
        for (User user : users) {
            String message = settings.getWhatsappMassiveTemplate()
                    .replace("{nombre}", user.getFullName())
                    .replace("{mensaje}", customMessage);
            
            String link = generateWhatsAppLink(user.getPhone(), message);
            if (link != null) {
                links.add(new WhatsAppLinkDto(
                        user.getId(),
                        user.getFullName(),
                        user.getPhone(),
                        link
                ));
            }
        }
        
        return links;
    }

    /**
     * Envia correo masivo a todos los usuarios o seleccionados
     */
    public MassiveEmailResult sendMassiveEmail(List<Long> userIds, String subject, String message) {
        List<User> users;
        
        if (userIds == null || userIds.isEmpty()) {
            // Todos los estudiantes con email
            users = userRepository.findAll().stream()
                    .filter(u -> u.getEmail() != null && !u.getEmail().isEmpty())
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAllById(userIds).stream()
                    .filter(u -> u.getEmail() != null && !u.getEmail().isEmpty())
                    .collect(Collectors.toList());
        }

        int sent = 0;
        int failed = 0;
        List<String> failedEmails = new ArrayList<>();

        for (User user : users) {
            boolean success = emailService.sendMassiveEmail(
                    user.getEmail(),
                    user.getFullName(),
                    subject,
                    message
            );
            
            if (success) {
                sent++;
            } else {
                failed++;
                failedEmails.add(user.getEmail());
            }
        }

        return new MassiveEmailResult(sent, failed, failedEmails);
    }

    /**
     * Obtiene usuarios con cursos por vencer en X dias
     */
    public List<UserCourseExpiryDto> getUsersWithExpiringCourses(int daysBeforeExpiry) {
        List<UserCourseExpiryDto> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limitDate = now.plusDays(daysBeforeExpiry);

        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            for (UserCourse uc : user.getUserCourses()) {
                if (uc.getExpiresAt() != null && uc.getActive()) {
                    long daysLeft = ChronoUnit.DAYS.between(now, uc.getExpiresAt());
                    
                    if (daysLeft > 0 && daysLeft <= daysBeforeExpiry) {
                        result.add(new UserCourseExpiryDto(
                                user.getId(),
                                user.getFullName(),
                                user.getEmail(),
                                user.getPhone(),
                                uc.getCourse().getTitle(),
                                (int) daysLeft,
                                uc.getExpiresAt()
                        ));
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Obtiene usuarios con cursos vencidos
     */
    public List<UserCourseExpiryDto> getUsersWithExpiredCourses() {
        List<UserCourseExpiryDto> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            for (UserCourse uc : user.getUserCourses()) {
                if (uc.getExpiresAt() != null && uc.getExpiresAt().isBefore(now)) {
                    result.add(new UserCourseExpiryDto(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            uc.getCourse().getTitle(),
                            0,
                            uc.getExpiresAt()
                    ));
                }
            }
        }
        
        return result;
    }

    // ========== DTOs internos ==========
    
    public record WhatsAppLinkDto(
            Long userId,
            String fullName,
            String phone,
            String whatsappLink
    ) {}

    public record MassiveEmailResult(
            int sent,
            int failed,
            List<String> failedEmails
    ) {}

    public record UserCourseExpiryDto(
            Long userId,
            String fullName,
            String email,
            String phone,
            String courseName,
            int daysLeft,
            LocalDateTime expiresAt
    ) {}
}
