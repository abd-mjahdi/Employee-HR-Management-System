package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.mail.InvitationEmailPayload;
import com.example.employeetimetracking.event.EmailOutboxQueuedEvent;
import com.example.employeetimetracking.model.entities.Company;
import com.example.employeetimetracking.model.entities.EmailOutbox;
import com.example.employeetimetracking.model.enums.EmailOutboxStatus;
import com.example.employeetimetracking.repository.EmailOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class EmailOutboxService {

    public static final String AGGREGATE_INVITATION = "INVITATION";
    public static final String EVENT_INVITATION_CREATED = "INVITATION_CREATED";

    private final EmailOutboxRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final int maxAttempts;

    public EmailOutboxService(
            EmailOutboxRepository emailOutboxRepository,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.mail.outbox.max-attempts:8}") int maxAttempts) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public EmailOutbox enqueueInvitation(
            Company company,
            Long invitationId,
            InvitationEmailPayload payload) {
        EmailOutbox outbox = new EmailOutbox();
        outbox.setCompany(company);
        outbox.setAggregateType(AGGREGATE_INVITATION);
        outbox.setAggregateId(invitationId);
        outbox.setEventType(EVENT_INVITATION_CREATED);
        outbox.setRecipient(payload.recipient());
        outbox.setPayload(writePayload(payload));
        outbox.setStatus(EmailOutboxStatus.PENDING);
        outbox.setAttemptCount(0);
        outbox.setMaxAttempts(maxAttempts);
        outbox.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC));
        outbox = emailOutboxRepository.save(outbox);
        eventPublisher.publishEvent(new EmailOutboxQueuedEvent(outbox.getId()));
        return outbox;
    }

    private String writePayload(InvitationEmailPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize invitation email payload", e);
        }
    }
}
