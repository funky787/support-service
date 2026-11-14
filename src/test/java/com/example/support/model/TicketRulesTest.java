package com.example.support.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketRulesTest {
    private Clock clock;
    private Ticket ticket;
    @BeforeEach void setup() {
        clock = Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"), ZoneOffset.UTC);
        ticket = new Ticket("Printer", "broken", null, Priority.HIGH,
            Instant.parse("2026-01-01T00:00:00Z"), clock);
    }
    @Test void initialStatusIsNew() { assertEquals(TicketStatus.NEW, ticket.getStatus()); }
    @Test void initialPriorityIsPreserved() { assertEquals(Priority.HIGH, ticket.getPriority()); }
    @Test void newCanStart() { ticket.changeStatus(TicketStatus.IN_PROGRESS, clock); assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus()); }
    @Test void progressCanResolve() {
        ticket.changeStatus(TicketStatus.IN_PROGRESS, clock); ticket.changeStatus(TicketStatus.RESOLVED, clock);
        assertEquals(clock.instant(), ticket.getClosedAt());
    }
    @Test void newCanCancel() { ticket.changeStatus(TicketStatus.CANCELLED, clock); assertEquals(TicketStatus.CANCELLED, ticket.getStatus()); }
    @Test void progressCanCancel() { ticket.changeStatus(TicketStatus.IN_PROGRESS, clock); ticket.changeStatus(TicketStatus.CANCELLED, clock); assertEquals(TicketStatus.CANCELLED, ticket.getStatus()); }
    @Test void cannotSkipProgress() { assertThrows(IllegalStateException.class, () -> ticket.changeStatus(TicketStatus.RESOLVED, clock)); }
    @Test void cancelledCannotReopen() { ticket.changeStatus(TicketStatus.CANCELLED, clock); assertThrows(IllegalStateException.class, () -> ticket.changeStatus(TicketStatus.IN_PROGRESS, clock)); }
    @Test void resolvedCannotBeEdited() { ticket.changeStatus(TicketStatus.IN_PROGRESS, clock); ticket.changeStatus(TicketStatus.RESOLVED, clock); assertThrows(IllegalStateException.class, () -> ticket.update("new", Priority.LOW, null)); }
    @Test void overdueWhenDueBeforeNow() { assertTrue(ticket.isOverdue(clock)); }
    @Test void notOverdueExactlyAtDeadline() { ticket.update("", Priority.LOW, clock.instant()); assertFalse(ticket.isOverdue(clock)); }
    @Test void resolvedNotOverdue() { ticket.changeStatus(TicketStatus.IN_PROGRESS, clock); ticket.changeStatus(TicketStatus.RESOLVED, clock); assertFalse(ticket.isOverdue(clock)); }
    @Test void updateActiveTicket() { ticket.update("changed", Priority.LOW, null); assertEquals("changed", ticket.getDescription()); assertEquals(Priority.LOW, ticket.getPriority()); }
}
