package com.example.employeetimetracking.exception;

public class InactiveTenantException extends RuntimeException {
    public InactiveTenantException(String message) {
        super(message);
    }
}
