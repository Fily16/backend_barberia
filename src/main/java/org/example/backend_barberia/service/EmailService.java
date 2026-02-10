package org.example.backend_barberia.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.entity.EmailConfig;
import org.example.backend_barberia.repository.EmailConfigRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfigRepository emailConfigRepository;

    /**
     * Obtiene la configuracion actual del correo
     */
    public Optional<EmailConfig> getEmailConfig() {
        return emailConfigRepository.getConfig();
    }

    /**
     * Guarda o actualiza la configuracion del correo.
     * Si no se envia appPassword (porque el frontend no lo devuelve), 
     * se conserva el password existente en la BD.
     */
    public EmailConfig saveEmailConfig(EmailConfig config) {
        config.setId(1L);
        
        // Si el appPassword viene null o vacio, conservar el existente de la BD
        if (config.getAppPassword() == null || config.getAppPassword().isBlank()) {
            emailConfigRepository.findById(1L).ifPresent(existing -> {
                config.setAppPassword(existing.getAppPassword());
            });
        }
        
        return emailConfigRepository.save(config);
    }

    /**
     * Crea un JavaMailSender dinamico basado en la configuracion guardada
     */
    private JavaMailSender createMailSender(EmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(config.getSenderEmail());
        mailSender.setPassword(config.getAppPassword().replace(" ", ""));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * Envia correo de bienvenida a un nuevo estudiante
     */
    public boolean sendWelcomeEmail(String toEmail, String studentName, String username, String password) {
        Optional<EmailConfig> configOpt = emailConfigRepository.getConfig();
        
        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            log.warn("Email no configurado o desactivado. No se enviara correo a: {}", toEmail);
            return false;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("Email del estudiante vacio. No se puede enviar correo.");
            return false;
        }

        EmailConfig config = configOpt.get();
        
        try {
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSenderEmail(), config.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(config.getWelcomeSubject());

            // Reemplazar variables en plantilla
            String title = config.getWelcomeTitle().replace("{nombre}", studentName);
            String content = config.getWelcomeMessage();

            String htmlContent = buildEmailHtml(config, title, content, username, password, 
                    config.getWelcomeButtonText(), config.getButtonUrl());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Correo de bienvenida enviado a: {}", toEmail);
            return true;

        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", toEmail, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error inesperado enviando correo: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia correo de vencimiento proximo
     */
    public boolean sendExpiringEmail(String toEmail, String studentName, String courseName, int daysLeft) {
        Optional<EmailConfig> configOpt = emailConfigRepository.getConfig();
        
        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            return false;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            return false;
        }

        EmailConfig config = configOpt.get();
        
        try {
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSenderEmail(), config.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(config.getExpiringSubject());

            String title = config.getExpiringTitle()
                    .replace("{nombre}", studentName);
            String content = config.getExpiringMessage()
                    .replace("{nombre}", studentName)
                    .replace("{curso}", courseName)
                    .replace("{dias}", String.valueOf(daysLeft));

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    config.getExpiringButtonText(), config.getButtonUrl());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Correo de vencimiento enviado a: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Error enviando correo de vencimiento: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia correo de curso vencido
     */
    public boolean sendExpiredEmail(String toEmail, String studentName, String courseName) {
        Optional<EmailConfig> configOpt = emailConfigRepository.getConfig();
        
        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            return false;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            return false;
        }

        EmailConfig config = configOpt.get();
        
        try {
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSenderEmail(), config.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(config.getExpiredSubject());

            String title = config.getExpiredTitle()
                    .replace("{nombre}", studentName);
            String content = config.getExpiredMessage()
                    .replace("{nombre}", studentName)
                    .replace("{curso}", courseName);

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    config.getExpiredButtonText(), config.getButtonUrl());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Correo de vencido enviado a: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Error enviando correo de vencido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia correo masivo personalizado
     */
    public boolean sendMassiveEmail(String toEmail, String studentName, String subject, String customMessage) {
        Optional<EmailConfig> configOpt = emailConfigRepository.getConfig();
        
        if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
            return false;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            return false;
        }

        EmailConfig config = configOpt.get();
        
        try {
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSenderEmail(), config.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String title = "¡Hola, " + studentName + "!";
            String content = customMessage.replace("{nombre}", studentName);

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    "VER MAS", config.getButtonUrl());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return true;

        } catch (Exception e) {
            log.error("Error enviando correo masivo a {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Prueba la configuracion de correo
     */
    public boolean sendTestEmail(String toEmail) {
        Optional<EmailConfig> configOpt = emailConfigRepository.getConfig();
        
        if (configOpt.isEmpty()) {
            log.error("No hay configuracion de correo guardada. Guarda la configuracion primero.");
            throw new RuntimeException("No hay configuración de correo guardada. Guarda la configuración primero.");
        }

        EmailConfig config = configOpt.get();
        
        if (config.getAppPassword() == null || config.getAppPassword().isBlank()) {
            log.error("No hay contraseña de aplicación configurada");
            throw new RuntimeException("No hay contraseña de aplicación configurada. Ingresa tu App Password de Gmail.");
        }
        
        if (config.getSenderEmail() == null || config.getSenderEmail().isBlank()) {
            log.error("No hay correo emisor configurado");
            throw new RuntimeException("No hay correo emisor configurado.");
        }
        
        try {
            JavaMailSender mailSender = createMailSender(config);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getSenderEmail(), config.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject("Prueba de configuracion - " + config.getSenderName());

            String htmlContent = buildEmailHtml(config, "¡Configuracion correcta!", 
                    "El sistema de correos esta funcionando correctamente.", null, null,
                    "IR A LA PLATAFORMA", config.getButtonUrl());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Correo de prueba enviado a: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Error en prueba de correo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar: " + e.getMessage());
        }
    }

    /**
     * Construye el HTML del correo usando la configuracion de plantilla
     */
    private String buildEmailHtml(EmailConfig config, String title, String message, 
                                   String username, String password, String buttonText, String buttonUrl) {
        
        String logoSection = "";
        if (config.getLogoUrl() != null && !config.getLogoUrl().isEmpty()) {
            logoSection = String.format("""
                <img src="%s" alt="%s" style="max-height: 60px; width: auto; margin-bottom: 15px;" />
                """, config.getLogoUrl(), config.getSenderName());
        }

        String credentialsSection = "";
        if (username != null && password != null) {
            credentialsSection = String.format("""
                <div style="background-color: #1e1e1e; border: 1px solid #333; border-radius: 8px; padding: 25px; margin: 25px 0;">
                    <h3 style="color: %s; margin: 0 0 15px; font-size: 14px; letter-spacing: 1.5px; text-transform: uppercase;">
                        Tus credenciales de acceso
                    </h3>
                    <p style="color: #ffffff; margin: 8px 0; font-size: 15px;">
                        <strong>Usuario:</strong> <span style="color: %s; font-family: monospace;">%s</span>
                    </p>
                    <p style="color: #ffffff; margin: 8px 0; font-size: 15px;">
                        <strong>Contraseña:</strong> <span style="color: %s; font-family: monospace;">%s</span>
                    </p>
                </div>
                <p style="color: #888; font-size: 13px; margin: 20px 0;">
                    ⚠️ Te recomendamos cambiar tu contraseña despues de tu primer inicio de sesion.
                </p>
                """, config.getPrimaryColor(), config.getPrimaryColor(), username, 
                config.getPrimaryColor(), password);
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: %s;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="max-width: 600px; margin: 0 auto; background-color: #141414;">
                    <!-- Header -->
                    <tr>
                        <td style="padding: 40px 30px; text-align: center; background: linear-gradient(135deg, #1a1a1a 0%%, #0a0a0a 100%%); border-bottom: 2px solid %s;">
                            %s
                            <h1 style="color: %s; margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 2px;">
                                %s
                            </h1>
                        </td>
                    </tr>
                    
                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <h2 style="color: %s; margin: 0 0 20px; font-size: 22px; font-weight: 500;">
                                %s
                            </h2>
                            
                            <p style="color: #b0b0b0; font-size: 16px; line-height: 1.6; margin: 0 0 25px;">
                                %s
                            </p>
                            
                            %s
                            
                            <!-- CTA Button -->
                            <div style="text-align: center; margin: 35px 0;">
                                <a href="%s" 
                                   style="display: inline-block; background-color: %s; color: %s; padding: 15px 40px; text-decoration: none; border-radius: 4px; font-weight: 600; font-size: 14px; letter-spacing: 1px; text-transform: uppercase;">
                                    %s
                                </a>
                            </div>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 25px 30px; background-color: #0a0a0a; border-top: 1px solid #222; text-align: center;">
                            <p style="color: #666; font-size: 12px; margin: 0;">
                                © 2025 %s. Todos los derechos reservados.
                            </p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
            config.getBackgroundColor(),
            config.getPrimaryColor(),
            logoSection,
            config.getPrimaryColor(),
            config.getSenderName(),
            config.getTextColor(),
            title,
            message,
            credentialsSection,
            buttonUrl,
            config.getPrimaryColor(),
            config.getBackgroundColor(),
            buttonText,
            config.getSenderName()
        );
    }
}
