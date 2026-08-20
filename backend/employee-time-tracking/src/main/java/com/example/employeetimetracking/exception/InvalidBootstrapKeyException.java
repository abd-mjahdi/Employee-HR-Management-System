package com.example.employeetimetracking.exception;

public class InvalidBootstrapKeyException extends AuthenticationException {
    public InvalidBootstrapKeyException() {
        super("Unauthorized");
    }
}
