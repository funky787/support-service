package com.example.support.dto;
import com.example.support.model.Priority;
import com.example.support.model.Ticket;
import com.example.support.model.TicketStatus;

import java.time.Instant;

public record TicketResponse(
    Long id, String title, String description, Long categoryId,
    Priority priority, TicketStatus status, Instant createdAt,
    Instant dueAt, Instant closedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
            ticket.getCategory().getId(), ticket.getPriority(), ticket.getStatus(),
            ticket.getCreatedAt(), ticket.getDueAt(), ticket.getClosedAt());
    }
}
