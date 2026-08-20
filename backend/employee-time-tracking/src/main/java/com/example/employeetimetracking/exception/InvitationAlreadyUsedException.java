package com.example.employeetimetracking.exception;

public class InvitationAlreadyUsedException extends RuntimeException {
    public InvitationAlreadyUsedException(String message) {
        super(message);
    }
}
