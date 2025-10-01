package com.creditdefaultswap.platform.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for trade amendments.
 */
public class AmendTradeRequest {
    private Map<String, String> amendments;
    private LocalDate amendmentDate;
    private String amendedBy;
    private String amendmentReason;

    public AmendTradeRequest() {}

    public AmendTradeRequest(Map<String, String> amendments, LocalDate amendmentDate, String amendedBy) {
        this.amendments = amendments;
        this.amendmentDate = amendmentDate;
        this.amendedBy = amendedBy;
    }

    public Map<String, String> getAmendments() {
        return amendments;
    }

    public void setAmendments(Map<String, String> amendments) {
        this.amendments = amendments;
    }

    public LocalDate getAmendmentDate() {
        return amendmentDate;
    }

    public void setAmendmentDate(LocalDate amendmentDate) {
        this.amendmentDate = amendmentDate;
    }

    public String getAmendedBy() {
        return amendedBy;
    }

    public void setAmendedBy(String amendedBy) {
        this.amendedBy = amendedBy;
    }

    public String getAmendmentReason() {
        return amendmentReason;
    }

    public void setAmendmentReason(String amendmentReason) {
        this.amendmentReason = amendmentReason;
    }
}