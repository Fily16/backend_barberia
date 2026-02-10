package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.entity.EmailConfig;
import org.example.backend_barberia.repository.EmailConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfigRepository emailConfigRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String resendApiKey;

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
            emailConfigRepository.findById(1L).ifPresent(existing ->
                config.setAppPassword(existing.getAppPassword())
            );
        }

        return emailConfigRepository.save(config);
    }

    // ==================== ENVIO DE CORREOS VIA RESEND API ====================

    /**
     * Envia un correo usando la API HTTP de Resend.
     * Funciona en cualquier hosting (Render, Railway, etc.) sin restricciones SMTP.
     */
    private boolean sendViaResend(String toEmail, String subject, String htmlContent, EmailConfig config) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("RESEND_API_KEY no configurada");
            throw new RuntimeException("RESEND_API_KEY no está configurada en el servidor.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // Resend free tier: from debe ser onboarding@resend.dev
            // El senderEmail se usa como reply-to para que las respuestas lleguen al correo real
            String fromEmail = config.getSenderName() + " <onboarding@resend.dev>";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", fromEmail);
            body.put("to", List.of(toEmail));
            body.put("subject", subject);
            body.put("html", htmlContent);
            body.put("reply_to", config.getSenderEmail());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    RESEND_API_URL, HttpMethod.POST, request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo enviado via Resend a: {}", toEmail);
                return true;
            } else {
                log.error("Resend respondio con status {}: {}", response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("Error enviando correo via Resend a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error al enviar correo: " + e.getMessage());
        }
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
            String title = config.getWelcomeTitle().replace("{nombre}", studentName);
            String content = config.getWelcomeMessage();

            String htmlContent = buildEmailHtml(config, title, content, username, password,
                    config.getWelcomeButtonText(), config.getButtonUrl());

            return sendViaResend(toEmail, config.getWelcomeSubject(), htmlContent, config);

        } catch (Exception e) {
            log.error("Error enviando correo de bienvenida a {}: {}", toEmail, e.getMessage());
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
            String title = config.getExpiringTitle()
                    .replace("{nombre}", studentName);
            String content = config.getExpiringMessage()
                    .replace("{nombre}", studentName)
                    .replace("{curso}", courseName)
                    .replace("{dias}", String.valueOf(daysLeft));

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    config.getExpiringButtonText(), config.getButtonUrl());

            return sendViaResend(toEmail, config.getExpiringSubject(), htmlContent, config);

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
            String title = config.getExpiredTitle()
                    .replace("{nombre}", studentName);
            String content = config.getExpiredMessage()
                    .replace("{nombre}", studentName)
                    .replace("{curso}", courseName);

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    config.getExpiredButtonText(), config.getButtonUrl());

            return sendViaResend(toEmail, config.getExpiredSubject(), htmlContent, config);

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
            String title = "¡Hola, " + studentName + "!";
            String content = customMessage.replace("{nombre}", studentName);

            String htmlContent = buildEmailHtml(config, title, content, null, null,
                    "VER MAS", config.getButtonUrl());

            return sendViaResend(toEmail, subject, htmlContent, config);

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
            throw new RuntimeException("No hay configuración de correo guardada. Guarda la configuración primero.");
        }

        EmailConfig config = configOpt.get();

        if (config.getSenderEmail() == null || config.getSenderEmail().isBlank()) {
            throw new RuntimeException("No hay correo emisor configurado.");
        }

        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new RuntimeException("RESEND_API_KEY no está configurada en el servidor. Agrega la variable de entorno en Render.");
        }

        String htmlContent = buildEmailHtml(config, "¡Configuración correcta!",
                "El sistema de correos está funcionando correctamente mediante Resend.", null, null,
                "IR A LA PLATAFORMA", config.getButtonUrl());

        return sendViaResend(toEmail, "Prueba de configuración - " + config.getSenderName(), htmlContent, config);
    }

    /**
     * Construye el HTML del correo usando la configuracion de plantilla
     */
    private String buildEmailHtml(EmailConfig config, String title, String message,
                                   String username, String password, String buttonText, String buttonUrl) {

        String logoSection = "";
        if (config.getLogoUrl() != null && !config.getLogoUrl().isEmpty()) {
            logoSection = String.format(
                "<img src=\"%s\" alt=\"%s\" style=\"max-height: 60px; width: auto; margin-bottom: 15px;\" />",
                config.getLogoUrl(), config.getSenderName());
        }

        String credentialsSection = "";
        if (username != null && password != null) {
            credentialsSection = "<div style=\"background-color: #1e1e1e; border: 1px solid #333; border-radius: 8px; padding: 25px; margin: 25px 0;\">"
                + "<h3 style=\"color: " + config.getPrimaryColor() + "; margin: 0 0 15px; font-size: 14px; letter-spacing: 1.5px; text-transform: uppercase;\">Tus credenciales de acceso</h3>"
                + "<p style=\"color: #ffffff; margin: 8px 0; font-size: 15px;\"><strong>Usuario:</strong> <span style=\"color: " + config.getPrimaryColor() + "; font-family: monospace;\">" + username + "</span></p>"
                + "<p style=\"color: #ffffff; margin: 8px 0; font-size: 15px;\"><strong>Contraseña:</strong> <span style=\"color: " + config.getPrimaryColor() + "; font-family: monospace;\">" + password + "</span></p>"
                + "</div>"
                + "<p style=\"color: #888; font-size: 13px; margin: 20px 0;\">⚠️ Te recomendamos cambiar tu contraseña después de tu primer inicio de sesión.</p>";
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
            + "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: " + config.getBackgroundColor() + ";\">"
            + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; margin: 0 auto; background-color: #141414;\">"
            + "<tr><td style=\"padding: 40px 30px; text-align: center; background: linear-gradient(135deg, #1a1a1a 0%, #0a0a0a 100%); border-bottom: 2px solid " + config.getPrimaryColor() + ";\">"
            + logoSection
            + "<h1 style=\"color: " + config.getPrimaryColor() + "; margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 2px;\">" + config.getSenderName() + "</h1>"
            + "</td></tr>"
            + "<tr><td style=\"padding: 40px 30px;\">"
            + "<h2 style=\"color: " + config.getTextColor() + "; margin: 0 0 20px; font-size: 22px; font-weight: 500;\">" + title + "</h2>"
            + "<p style=\"color: #b0b0b0; font-size: 16px; line-height: 1.6; margin: 0 0 25px;\">" + message + "</p>"
            + credentialsSection
            + "<div style=\"text-align: center; margin: 35px 0;\">"
            + "<a href=\"" + buttonUrl + "\" style=\"display: inline-block; background-color: " + config.getPrimaryColor() + "; color: " + config.getBackgroundColor() + "; padding: 15px 40px; text-decoration: none; border-radius: 4px; font-weight: 600; font-size: 14px; letter-spacing: 1px; text-transform: uppercase;\">" + buttonText + "</a>"
            + "</div></td></tr>"
            + "<tr><td style=\"padding: 25px 30px; background-color: #0a0a0a; border-top: 1px solid #222; text-align: center;\">"
            + "<p style=\"color: #666; font-size: 12px; margin: 0;\">© 2025 " + config.getSenderName() + ". Todos los derechos reservados.</p>"
            + "</td></tr></table></body></html>";
    }
}