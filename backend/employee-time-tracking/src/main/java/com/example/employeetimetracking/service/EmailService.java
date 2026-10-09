package com.example.employeetimetracking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
public class EmailService {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'at' HH:mm 'UTC'", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final String from;
    private final String tenantUrlTemplate;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend.tenant-url-template}") String tenantUrlTemplate) {
        this.mailSender = mailSender;
        this.from = from;
        this.tenantUrlTemplate = tenantUrlTemplate;
    }

    public void sendInviteEmail(
            String companyName,
            String companySlug,
            String recipient,
            String rawToken,
            String expiresAt) {

        String inviteUrl = buildInviteUrl(companySlug, rawToken);
        String expiryLabel = formatExpiry(expiresAt);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("You're invited to join " + companyName);
            helper.setText(plainBody(companyName, inviteUrl, expiryLabel), htmlBody(companyName, inviteUrl, expiryLabel));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to compose invitation email", e);
        }
    }

    String buildInviteUrl(String companySlug, String rawToken) {
        String tenantUrl = tenantUrlTemplate.replace("{slug}", companySlug);

        return UriComponentsBuilder.fromUriString(tenantUrl)
                .path("/invite")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    private static String plainBody(String companyName, String inviteUrl, String expiryLabel) {
        return "Hello,\n\n"
                + companyName + " has invited you to join their workspace on HRApp.\n\n"
                + "Accept the invitation here:\n"
                + inviteUrl + "\n\n"
                + "This link expires on " + expiryLabel + ".\n"
                + "If the link does not open, copy and paste it into your browser.\n\n"
                + "If you were not expecting this invitation, you can ignore this email.\n\n"
                + "Thanks,\n"
                + companyName;
    }

    private static String htmlBody(String companyName, String inviteUrl, String expiryLabel) {
        String safeCompany = escape(companyName);
        String safeUrl = escape(inviteUrl);
        String safeExpiry = escape(expiryLabel);
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#1f2937;">
                  <p>Hello,</p>
                  <p><strong>%s</strong> has invited you to join their workspace on HRApp.</p>
                  <p>
                    <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:10px 16px;border-radius:6px;">
                      Accept invitation
                    </a>
                  </p>
                  <p>This link expires on <strong>%s</strong>.</p>
                  <p style="font-size:13px;color:#4b5563;">If the button does not work, copy this URL into your browser:<br>%s</p>
                  <p>If you were not expecting this invitation, you can ignore this email.</p>
                  <p>Thanks,<br>%s</p>
                </div>
                """.formatted(safeCompany, safeUrl, safeExpiry, safeUrl, safeCompany);
    }

    public static String formatExpiry(String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank()) {
            return "72 hours after it was sent";
        }
        try {
            return LocalDateTime.parse(expiresAt).format(EXPIRY_FORMAT);
        } catch (DateTimeParseException e) {
            return expiresAt;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
