package com.example.support.service;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.StatusRequest;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.StatusHistory;
import com.example.support.model.Ticket;
import com.example.support.model.TicketStatus;
import com.example.support.repository.CategoryRepository;
import com.example.support.repository.StatusHistoryRepository;
import com.example.support.repository.TicketRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;

class TicketServiceTest {
    @Mock TicketRepository tickets;
    @Mock CategoryRepository categories;
    @Mock StatusHistoryRepository history;
    private TicketService service;
    private Clock clock = Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"), ZoneOffset.UTC);
    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); service = new TicketService(tickets, categories, history, clock); }
    @Test void notFoundReturns404() {
        when(tickets.findById(42L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.get(42L)).getStatusCode().value());
    }
    @Test void unknownCategoryReturns400() {
        when(categories.findById(99L)).thenReturn(Optional.empty());
        assertEquals(400, assertThrows(ResponseStatusException.class,
            () -> service.create(new CreateTicketRequest("Title", null, 99L, null, null))).getStatusCode().value());
    }
    @Test void forbiddenTransitionDoesNotWriteHistory() {
        Ticket t = new Ticket("Title", null, null, Priority.LOW, null, clock);
        when(tickets.findById(1L)).thenReturn(Optional.of(t));
        assertEquals(409, assertThrows(ResponseStatusException.class,
            () -> service.changeStatus(1L, new StatusRequest(TicketStatus.RESOLVED))).getStatusCode().value());
        verifyNoInteractions(history);
        assertEquals(TicketStatus.NEW, t.getStatus());
    }
    @Test void allowedTransitionWritesHistory() {
        Category c = mock(Category.class);
        when(c.getId()).thenReturn(1L);
        Ticket t = new Ticket("Title", null, c, Priority.LOW, null, clock);
        when(tickets.findById(1L)).thenReturn(Optional.of(t));
        assertEquals(TicketStatus.IN_PROGRESS, service.changeStatus(1L, new StatusRequest(TicketStatus.IN_PROGRESS)).status());
        verify(history).save(any(StatusHistory.class));
    }
}
