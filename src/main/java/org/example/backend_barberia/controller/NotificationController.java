package org.example.backend_barberia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.entity.EmailConfig;
import org.example.backend_barberia.entity.NotificationSettings;
import org.example.backend_barberia.service.EmailService;
import org.example.backend_barberia.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Sistema de notificaciones por Email y WhatsApp")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailService emailService;

    // ==================== CONFIGURACION EMAIL ====================

    @GetMapping("/email/config")
    @Operation(summary = "Obtener config de email")
    public ResponseEntity<?> getEmailConfig() {
        return emailService.getEmailConfig()
                .map(config -> ResponseEntity.ok(Map.of(
                        "configured", true,
                        "senderEmail", config.getSenderEmail(),
                        "senderName", config.getSenderName(),
                        "enabled", config.getEnabled(),
                        "logoUrl", config.getLogoUrl() != null ? config.getLogoUrl() : "",
                        "primaryColor", config.getPrimaryColor(),
                        "backgroundColor", config.getBackgroundColor(),
                        "textColor", config.getTextColor(),
                        "buttonUrl", config.getButtonUrl(),
                        "welcomeSubject", config.getWelcomeSubject(),
                        "welcomeTitle", config.getWelcomeTitle(),
                        "welcomeMessage", config.getWelcomeMessage(),
                        "welcomeButtonText", config.getWelcomeButtonText(),
                        "expiringSubject", config.getExpiringSubject(),
                        "expiringTitle", config.getExpiringTitle(),
                        "expiringMessage", config.getExpiringMessage(),
                        "expiringButtonText", config.getExpiringButtonText(),
                        "expiredSubject", config.getExpiredSubject(),
                        "expiredTitle", config.getExpiredTitle(),
                        "expiredMessage", config.getExpiredMessage(),
                        "expiredButtonText", config.getExpiredButtonText()
                )))
                .orElse(ResponseEntity.ok(Map.of("configured", false)));
    }

    @PostMapping("/email/config")
    @Operation(summary = "Guardar config de email")
    public ResponseEntity<?> saveEmailConfig(@RequestBody EmailConfig config) {
        try {
            EmailConfig saved = emailService.saveEmailConfig(config);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuracion de email guardada"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/email/test")
    @Operation(summary = "Enviar email de prueba")
    public ResponseEntity<?> sendTestEmail(@RequestParam String toEmail) {
        boolean success = emailService.sendTestEmail(toEmail);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Correo de prueba enviado" : "Error al enviar correo"
        ));
    }

    // ==================== CONFIGURACION NOTIFICACIONES ====================

    @GetMapping("/settings")
    @Operation(summary = "Obtener config de notificaciones")
    public ResponseEntity<NotificationSettings> getSettings() {
        return ResponseEntity.ok(notificationService.getSettings());
    }

    @PostMapping("/settings")
    @Operation(summary = "Guardar config de notificaciones")
    public ResponseEntity<?> saveSettings(@RequestBody NotificationSettings settings) {
        try {
            NotificationSettings saved = notificationService.saveSettings(settings);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuracion guardada",
                    "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ==================== ENVIO MASIVO EMAIL ====================

    @PostMapping("/email/massive")
    @Operation(summary = "Enviar email masivo")
    public ResponseEntity<?> sendMassiveEmail(@RequestBody MassiveEmailRequest request) {
        var result = notificationService.sendMassiveEmail(
                request.userIds(),
                request.subject(),
                request.message()
        );
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "sent", result.sent(),
                "failed", result.failed(),
                "failedEmails", result.failedEmails()
        ));
    }

    // ==================== WHATSAPP LINKS ====================

    @PostMapping("/whatsapp/links")
    @Operation(summary = "Generar links de WhatsApp para envio masivo")
    public ResponseEntity<?> generateWhatsAppLinks(@RequestBody WhatsAppLinksRequest request) {
        var links = notificationService.generateMassiveWhatsAppLinks(
                request.userIds(),
                request.message()
        );
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "totalLinks", links.size(),
                "links", links
        ));
    }

    // ==================== USUARIOS CON CURSOS POR VENCER ====================

    @GetMapping("/expiring")
    @Operation(summary = "Obtener usuarios con cursos por vencer")
    public ResponseEntity<?> getExpiringCourses(@RequestParam(defaultValue = "7") int days) {
        var users = notificationService.getUsersWithExpiringCourses(days);
        return ResponseEntity.ok(Map.of(
                "total", users.size(),
                "users", users
        ));
    }

    @GetMapping("/expired")
    @Operation(summary = "Obtener usuarios con cursos vencidos")
    public ResponseEntity<?> getExpiredCourses() {
        var users = notificationService.getUsersWithExpiredCourses();
        return ResponseEntity.ok(Map.of(
                "total", users.size(),
                "users", users
        ));
    }

    @PostMapping("/expiring/notify")
    @Operation(summary = "Enviar notificaciones a usuarios con cursos por vencer")
    public ResponseEntity<?> notifyExpiringCourses(@RequestParam(defaultValue = "7") int days) {
        var users = notificationService.getUsersWithExpiringCourses(days);
        
        int emailsSent = 0;
        for (var user : users) {
            if (user.email() != null && !user.email().isEmpty()) {
                boolean sent = emailService.sendExpiringEmail(
                        user.email(),
                        user.fullName(),
                        user.courseName(),
                        user.daysLeft()
                );
                if (sent) emailsSent++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "usersFound", users.size(),
                "emailsSent", emailsSent
        ));
    }

    // ==================== DTOs ====================

    public record MassiveEmailRequest(
            List<Long> userIds,  // null o vacio = todos
            String subject,
            String message
    ) {}

    public record WhatsAppLinksRequest(
            List<Long> userIds,  // null o vacio = todos
            String message
    ) {}
}
