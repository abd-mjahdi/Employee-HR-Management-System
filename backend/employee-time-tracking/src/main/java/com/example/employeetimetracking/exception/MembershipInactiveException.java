package com.example.employeetimetracking.exception;

public class MembershipInactiveException extends AuthenticationException {
    public MembershipInactiveException(String message) {
        super(message);
    }
}
