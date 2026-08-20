package com.example.employeetimetracking.exception;

public class BootstrapAlreadyCompletedException extends RuntimeException {
    public BootstrapAlreadyCompletedException() {
        super("Bootstrap already completed");
    }
}
