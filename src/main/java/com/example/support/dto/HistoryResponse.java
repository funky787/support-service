package com.example.support.dto;
import com.example.support.model.StatusHistory;
import com.example.support.model.TicketStatus;

import java.time.Instant;
public record HistoryResponse(Long id, TicketStatus oldStatus, TicketStatus newStatus, Instant changedAt) {
    public static HistoryResponse from(StatusHistory h) {
        return new HistoryResponse(h.getId(), h.getOldStatus(), h.getNewStatus(), h.getChangedAt());
    }
}
