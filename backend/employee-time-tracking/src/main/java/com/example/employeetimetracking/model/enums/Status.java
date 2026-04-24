package com.example.employeetimetracking.model.enums;

public enum Status {
    PENDING,
    APPROVED,
    DENIED,
    CANCELLED,
    /** Approved entry locked; employee asked to change; waiting for supervisor to allow edit */
    PENDING_CORRECTION
}
