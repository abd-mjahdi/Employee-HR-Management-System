package com.example.employeetimetracking.jobs;

import com.example.employeetimetracking.dto.mail.InvitationEmailPayload;
import com.example.employeetimetracking.model.entities.EmailOutbox;
import com.example.employeetimetracking.model.enums.EmailOutboxStatus;
import com.example.employeetimetracking.repository.EmailOutboxRepository;
import com.example.employeetimetracking.service.EmailOutboxService;
import com.example.employeetimetracking.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class EmailOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxProcessor.class);

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public EmailOutboxProcessor(
            EmailOutboxRepository emailOutboxRepository,
            EmailService emailService,
            ObjectMapper objectMapper,
            @Value("${app.mail.outbox.batch-size:20}") int batchSize) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox.poll-interval-ms:10000}")
    @Transactional
    public void processDue() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<EmailOutbox> due = emailOutboxRepository.lockDue(now, batchSize);
        for (EmailOutbox item : due) {
            deliver(item);
        }
    }

    @Transactional
    public void processOne(Long outboxId) {
        emailOutboxRepository.findByIdForUpdate(outboxId).ifPresent(this::deliver);
    }

    private void deliver(EmailOutbox item) {
        if (item.getStatus() != EmailOutboxStatus.PENDING) {
            return;
        }
        if (item.getNextAttemptAt() != null
                && item.getNextAttemptAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            return;
        }

        try {
            if (EmailOutboxService.EVENT_INVITATION_CREATED.equals(item.getEventType())) {
                InvitationEmailPayload payload = readInvitationPayload(item.getPayload());
                emailService.sendInviteEmail(
                        payload.companyName(),
                        payload.companySlug(),
                        payload.recipient(),
                        payload.rawToken(),
                        payload.expiresAt()
                );
                item.setPayload(objectMapper.writeValueAsString(payload.withoutToken()));
            } else {
                throw new IllegalStateException("Unsupported outbox event type: " + item.getEventType());
            }
            item.setStatus(EmailOutboxStatus.SENT);
            item.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
            item.setLastError(null);
        } catch (Exception ex) {
            int attempts = item.getAttemptCount() + 1;
            item.setAttemptCount(attempts);
            item.setLastError(truncate(ex.getMessage()));
            if (attempts >= item.getMaxAttempts()) {
                item.setStatus(EmailOutboxStatus.FAILED);
                log.error("Email outbox id={} exhausted retries", item.getId(), ex);
            } else {
                item.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(backoffMinutes(attempts)));
                log.warn("Email outbox id={} send failed, attempt {}/{}",
                        item.getId(), attempts, item.getMaxAttempts(), ex);
            }
        }
    }

    private InvitationEmailPayload readInvitationPayload(String json) {
        try {
            InvitationEmailPayload payload = objectMapper.readValue(json, InvitationEmailPayload.class);
            if (payload.rawToken() == null || payload.rawToken().isBlank()) {
                throw new IllegalStateException("Invitation outbox payload is missing the raw token");
            }
            return payload;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read invitation email payload", e);
        }
    }

    static long backoffMinutes(int attemptCount) {
        return Math.min(60L, 1L << Math.min(attemptCount, 6));
    }

    private static String truncate(String message) {
        if (message == null) {
            return "Email send failed";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
