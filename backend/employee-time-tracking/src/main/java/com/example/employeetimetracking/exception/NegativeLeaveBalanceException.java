package com.example.employeetimetracking.exception;

public class NegativeLeaveBalanceException extends RuntimeException {
    public NegativeLeaveBalanceException(String message) {
        super(message);
    }
}
