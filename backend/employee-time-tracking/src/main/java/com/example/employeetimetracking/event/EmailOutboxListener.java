package com.example.employeetimetracking.event;

import com.example.employeetimetracking.jobs.EmailOutboxProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailOutboxListener {

    private final EmailOutboxProcessor emailOutboxProcessor;

    public EmailOutboxListener(EmailOutboxProcessor emailOutboxProcessor) {
        this.emailOutboxProcessor = emailOutboxProcessor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(EmailOutboxQueuedEvent event) {
        emailOutboxProcessor.processOne(event.outboxId());
    }
}
