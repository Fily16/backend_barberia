package org.example.backend_barberia.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {

    @Id
    private Long id;

    // ========== CONFIGURACION GENERAL ==========
    @Builder.Default
    private String adminWhatsApp = "";  // Numero del admin para enviar WhatsApp

    // ========== NOTIFICACIONES AUTOMATICAS - EMAIL ==========
    @Builder.Default
    private Boolean emailOnWelcome = true;  // Enviar email de bienvenida

    @Builder.Default
    private Boolean emailOnExpiringSoon = true;  // Enviar email cuando esta por vencer

    @Builder.Default
    private Integer emailDaysBeforeExpiry = 3;  // Dias antes de vencer para notificar

    @Builder.Default
    private Boolean emailOnExpired = true;  // Enviar email cuando vence

    // ========== NOTIFICACIONES AUTOMATICAS - WHATSAPP ==========
    @Builder.Default
    private Boolean whatsappOnWelcome = false;  // Enviar WhatsApp de bienvenida

    @Builder.Default
    private Boolean whatsappOnExpiringSoon = false;  // Enviar WhatsApp cuando esta por vencer

    @Builder.Default
    private Integer whatsappDaysBeforeExpiry = 3;  // Dias antes de vencer

    @Builder.Default
    private Boolean whatsappOnExpired = false;  // Enviar WhatsApp cuando vence

    // ========== PLANTILLAS WHATSAPP ==========
    @Column(length = 1000)
    @Builder.Default
    private String whatsappWelcomeTemplate = "¡Hola {nombre}! 👋\n\nBienvenido a *Ralph Cuts Academy*.\n\nTus credenciales:\n👤 Usuario: {usuario}\n🔑 Contraseña: {password}\n\n🔗 Ingresa aquí: {url}\n\n¡Éxitos en tu formación! 💈";

    @Column(length = 1000)
    @Builder.Default
    private String whatsappExpiringTemplate = "¡Hola {nombre}! ⚠️\n\nTu acceso al curso *{curso}* vence en *{dias} días*.\n\nRenueva ahora para seguir aprendiendo.\n\n🔗 {url}";

    @Column(length = 1000)
    @Builder.Default
    private String whatsappExpiredTemplate = "¡Hola {nombre}! 😢\n\nTu acceso al curso *{curso}* ha vencido.\n\nRenueva para continuar con tu formación profesional.\n\n🔗 {url}";

    @Column(length = 1000)
    @Builder.Default
    private String whatsappMassiveTemplate = "¡Hola {nombre}! 👋\n\n{mensaje}\n\n- Ralph Cuts Academy 💈";

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = 1L;
        }
    }
}
