package org.example.backend_barberia.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfig {

    @Id
    private Long id;

    // ========== CONFIGURACION SMTP ==========
    @Column(nullable = false)
    private String senderEmail;

    @Column(nullable = false)
    private String appPassword;

    @Column(nullable = false)
    @Builder.Default
    private String senderName = "Ralph Cuts Academy";

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // ========== PERSONALIZACION DE MARCA ==========
    @Column(length = 500)
    @Builder.Default
    private String logoUrl = "";

    @Builder.Default
    private String primaryColor = "#c9a227";

    @Builder.Default
    private String backgroundColor = "#0a0a0a";

    @Builder.Default
    private String textColor = "#ffffff";

    @Builder.Default
    private String buttonUrl = "https://ralph-cuts-academy.vercel.app/curso";

    // ========== PLANTILLA BIENVENIDA ==========
    @Builder.Default
    private String welcomeSubject = "Bienvenido a Ralph Cuts Academy";

    @Builder.Default
    private String welcomeTitle = "¡Bienvenido, {nombre}! 🎉";

    @Column(length = 1000)
    @Builder.Default
    private String welcomeMessage = "Tu cuenta ha sido creada exitosamente. Ahora tienes acceso a los mejores cursos de barberia profesional.";

    @Builder.Default
    private String welcomeButtonText = "INGRESAR A MI CURSO";

    // ========== PLANTILLA VENCIMIENTO PROXIMO ==========
    @Builder.Default
    private String expiringSubject = "Tu acceso esta por vencer - Ralph Cuts Academy";

    @Builder.Default
    private String expiringTitle = "¡{nombre}, tu acceso vence pronto!";

    @Column(length = 1000)
    @Builder.Default
    private String expiringMessage = "Tu acceso al curso {curso} vence en {dias} dias. Renueva ahora para seguir aprendiendo.";

    @Builder.Default
    private String expiringButtonText = "RENOVAR ACCESO";

    // ========== PLANTILLA CURSO VENCIDO ==========
    @Builder.Default
    private String expiredSubject = "Tu acceso ha vencido - Ralph Cuts Academy";

    @Builder.Default
    private String expiredTitle = "{nombre}, tu acceso ha expirado";

    @Column(length = 1000)
    @Builder.Default
    private String expiredMessage = "Tu acceso al curso {curso} ha vencido. Renueva para continuar con tu formacion profesional.";

    @Builder.Default
    private String expiredButtonText = "RENOVAR AHORA";

    // ========== PLANTILLA CORREO MASIVO ==========
    @Builder.Default
    private String massiveSubject = "Mensaje de Ralph Cuts Academy";

    @Column(length = 2000)
    @Builder.Default
    private String massiveTemplate = "";

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = 1L;
        }
    }
}
