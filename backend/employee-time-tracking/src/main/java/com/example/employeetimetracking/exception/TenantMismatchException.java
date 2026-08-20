package com.example.employeetimetracking.exception;

public class TenantMismatchException extends AuthenticationException {
    public TenantMismatchException(String message) {
        super(message);
    }
}
