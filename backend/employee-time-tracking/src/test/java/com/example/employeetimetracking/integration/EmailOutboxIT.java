package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.dto.request.CreateInvitationRequestDto;
import com.example.employeetimetracking.integration.persistence.AbstractPostgresIT;
import com.example.employeetimetracking.jobs.EmailOutboxProcessor;
import com.example.employeetimetracking.model.entities.EmailOutbox;
import com.example.employeetimetracking.model.enums.EmailOutboxStatus;
import com.example.employeetimetracking.model.enums.InvitationStatus;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.EmailOutboxRepository;
import com.example.employeetimetracking.repository.InvitationRepository;
import com.example.employeetimetracking.service.EmailOutboxService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailOutboxIT extends AbstractPostgresIT {

    @Autowired
    EmailOutboxRepository emailOutboxRepository;

    @Autowired
    InvitationRepository invitationRepository;

    @Autowired
    EmailOutboxProcessor emailOutboxProcessor;

    @SpyBean
    EmailOutboxService emailOutboxService;

    @AfterEach
    void restoreOutboxService() {
        doCallRealMethod().when(emailOutboxService).enqueueInvitation(any(), any(), any());
    }

    @Test
    void createInvitation_rollsBackInvitationAndOutboxTogether() throws Exception {
        String email = "outbox.rollback@example.com";
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("forced failure after outbox insert");
        }).when(emailOutboxService).enqueueInvitation(any(), any(), any());

        String hrToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        mockMvc.perform(post("/invitations")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateInvitationRequestDto(email, UserRole.EMPLOYEE, 1L, 3L))))
                .andExpect(status().is5xxServerError());

        assertTrue(invitationRepository.findByCompanyIdAndEmailAndStatus(1L, email, InvitationStatus.PENDING).isEmpty());
        assertTrue(emailOutboxRepository.findAll().stream().noneMatch(row -> email.equals(row.getRecipient())));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void createInvitation_keepsInvitationWhenSendFailsThenRetriesSuccessfully() throws Exception {
        String email = "outbox.retry@example.com";
        doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(javaMailSender).send(any(MimeMessage.class));

        String hrToken = login(ACME_HOST, ACME_HR_EMAIL, SEED_PASSWORD);
        mockMvc.perform(post("/invitations")
                        .with(tenantHost(ACME_HOST))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hrToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateInvitationRequestDto(email, UserRole.EMPLOYEE, 1L, 3L))))
                .andExpect(status().isCreated());

        assertEquals(1, invitationRepository.findByCompanyIdAndEmailAndStatus(1L, email, InvitationStatus.PENDING).size());
        EmailOutbox outbox = emailOutboxRepository.findAll().stream()
                .filter(row -> email.equals(row.getRecipient()))
                .findFirst()
                .orElseThrow();
        assertEquals(EmailOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(1, outbox.getAttemptCount());

        outbox.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        emailOutboxRepository.saveAndFlush(outbox);
        emailOutboxProcessor.processDue();

        EmailOutbox retried = emailOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertEquals(EmailOutboxStatus.SENT, retried.getStatus());
        assertTrue(retried.getPayload() == null || !retried.getPayload().contains("rawToken"));
    }
}
