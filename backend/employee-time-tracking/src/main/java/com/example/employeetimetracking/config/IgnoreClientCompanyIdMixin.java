package com.example.employeetimetracking.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mix-in so JSON {@code companyId} / {@code company_id} is never bound from a client payload.
 */
@JsonIgnoreProperties({"companyId", "company_id"})
public abstract class IgnoreClientCompanyIdMixin {
}
