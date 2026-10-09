package com.example.employeetimetracking.event;

import com.example.employeetimetracking.service.EmailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvitationEmailListener {

    private final EmailService emailService;

    public InvitationEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onInvitationCreated(InvitationCreatedEvent event) {
        emailService.sendInviteEmail(
                event.companyName(),
                event.companySlug(),
                event.recipient(),
                event.rawToken()
        );
    }
}