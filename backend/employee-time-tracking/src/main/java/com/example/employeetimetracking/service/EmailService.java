package com.example.employeetimetracking.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EmailService {

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
            String rawToken) {

        String inviteUrl = buildInviteUrl(companySlug, rawToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("You've been invited to join " + companyName + " on HRApp");
        message.setText(
                "Hello,\n\n" +
                        "You've been invited to join " + companyName + " on HRApp.\n\n" +
                        "Accept your invitation using this link:\n" +
                        inviteUrl + "\n\n" +
                        "If you weren't expecting this invitation, you can ignore this email.\n\n" +
                        "Best regards,\n" +
                        companyName + " via HRApp"
        );

        mailSender.send(message);
    }

    private String buildInviteUrl(String companySlug, String rawToken) {
        String tenantUrl = tenantUrlTemplate.replace("{slug}", companySlug);

        return UriComponentsBuilder.fromUriString(tenantUrl)
                .path("/invite")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }
}