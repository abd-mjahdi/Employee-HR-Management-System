package com.example.employeetimetracking.unit.service.mail;

import com.example.employeetimetracking.dto.mail.InvitationEmailPayload;
import com.example.employeetimetracking.jobs.EmailOutboxProcessor;
import com.example.employeetimetracking.model.entities.EmailOutbox;
import com.example.employeetimetracking.model.enums.EmailOutboxStatus;
import com.example.employeetimetracking.repository.EmailOutboxRepository;
import com.example.employeetimetracking.service.EmailOutboxService;
import com.example.employeetimetracking.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxProcessorTest {

    @Mock EmailOutboxRepository emailOutboxRepository;
    @Mock EmailService emailService;

    ObjectMapper objectMapper = new ObjectMapper();
    EmailOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EmailOutboxProcessor(emailOutboxRepository, emailService, objectMapper, 20);
    }

    @Test
    void processOne_marksSentAndRedactsToken() throws Exception {
        EmailOutbox item = pendingOutbox();
        when(emailOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        processor.processOne(1L);

        verify(emailService).sendInviteEmail(eq("Acme"), eq("acme"), eq("new@acme.com"), eq("raw-token"), anyString());
        assertEquals(EmailOutboxStatus.SENT, item.getStatus());
        InvitationEmailPayload stored = objectMapper.readValue(item.getPayload(), InvitationEmailPayload.class);
        assertNull(stored.rawToken());
        assertEquals("new@acme.com", stored.recipient());
    }

    @Test
    void processOne_keepsPendingAndSchedulesRetryOnSendFailure() throws Exception {
        EmailOutbox item = pendingOutbox();
        when(emailOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        doThrow(new RuntimeException("smtp down")).when(emailService)
                .sendInviteEmail(anyString(), anyString(), anyString(), anyString(), any());

        processor.processOne(1L);

        assertEquals(EmailOutboxStatus.PENDING, item.getStatus());
        assertEquals(1, item.getAttemptCount());
        assertTrue(item.getNextAttemptAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)));
        assertTrue(item.getPayload().contains("raw-token"));
    }

    @Test
    void processOne_marksFailedWhenRetriesExhausted() throws Exception {
        EmailOutbox item = pendingOutbox();
        item.setAttemptCount(7);
        item.setMaxAttempts(8);
        when(emailOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        doThrow(new RuntimeException("smtp down")).when(emailService)
                .sendInviteEmail(anyString(), anyString(), anyString(), anyString(), any());

        processor.processOne(1L);

        assertEquals(EmailOutboxStatus.FAILED, item.getStatus());
        assertEquals(8, item.getAttemptCount());
    }

    @Test
    void processOne_skipsAlreadySentToAvoidDuplicateDelivery() throws Exception {
        EmailOutbox item = pendingOutbox();
        item.setStatus(EmailOutboxStatus.SENT);
        when(emailOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        processor.processOne(1L);

        verify(emailService, never()).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), any());
        assertEquals(EmailOutboxStatus.SENT, item.getStatus());
    }

    @Test
    void processDue_deliversLockedPendingBatch() throws Exception {
        EmailOutbox item = pendingOutbox();
        when(emailOutboxRepository.lockDue(any(), eq(20))).thenReturn(List.of(item));

        processor.processDue();

        verify(emailService).sendInviteEmail(eq("Acme"), eq("acme"), eq("new@acme.com"), eq("raw-token"), anyString());
        assertEquals(EmailOutboxStatus.SENT, item.getStatus());
    }

    private EmailOutbox pendingOutbox() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(72);
        EmailOutbox item = new EmailOutbox();
        item.setId(1L);
        item.setEventType(EmailOutboxService.EVENT_INVITATION_CREATED);
        item.setRecipient("new@acme.com");
        item.setPayload(objectMapper.writeValueAsString(new InvitationEmailPayload(
                "Acme", "acme", "new@acme.com", "raw-token", expiresAt.toString())));
        item.setStatus(EmailOutboxStatus.PENDING);
        item.setAttemptCount(0);
        item.setMaxAttempts(8);
        item.setNextAttemptAt(expiresAt.minusHours(72));
        return item;
    }
}
