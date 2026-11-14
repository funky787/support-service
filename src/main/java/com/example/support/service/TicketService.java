package com.example.support.service;
import com.example.support.dto.CategoryResponse;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.EditTicketRequest;
import com.example.support.dto.HistoryResponse;
import com.example.support.dto.StatusRequest;
import com.example.support.dto.TicketResponse;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.StatusHistory;
import com.example.support.model.Ticket;
import com.example.support.model.TicketStatus;
import com.example.support.repository.CategoryRepository;
import com.example.support.repository.StatusHistoryRepository;
import com.example.support.repository.TicketRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final StatusHistoryRepository history;
    private final Clock clock;
    public TicketService(TicketRepository tickets, CategoryRepository categories,
                         StatusHistoryRepository history, Clock clock) {
        this.tickets = tickets; this.categories = categories; this.history = history; this.clock = clock;
    }
    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Category category = categories.findById(req.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        Priority priority = req.priority() == null ? Priority.MEDIUM : req.priority();
        Ticket ticket = tickets.save(new Ticket(req.title(), req.description(), category, priority, req.dueAt(), clock));
        return TicketResponse.from(ticket);
    }
    @Transactional(readOnly = true)
    public TicketResponse get(Long id) { return TicketResponse.from(require(id)); }
    private Ticket require(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }
    @Transactional
    public TicketResponse edit(Long id, EditTicketRequest req) {
        Ticket ticket = require(id);
        try { ticket.update(req.description(), req.priority(), req.dueAt()); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        return TicketResponse.from(ticket);
    }
    @Transactional
    public TicketResponse changeStatus(Long id, StatusRequest req) {
        Ticket ticket = require(id);
        TicketStatus old = ticket.getStatus();
        try { ticket.changeStatus(req.status(), clock); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        history.save(new StatusHistory(ticket, old, req.status(), clock.instant()));
        return TicketResponse.from(ticket);
    }
    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long id) {
        require(id);
        return history.findByTicketIdOrderByChangedAtAscIdAsc(id).stream().map(HistoryResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categories.findAll().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }
    @Transactional(readOnly = true)
    public Page<TicketResponse> list(TicketStatus status, Priority priority, Long categoryId,
                                     Boolean overdue, Pageable pageable) {
        Specification<Ticket> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (priority != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        if (categoryId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        if (overdue != null) {
            Instant now = clock.instant();
            spec = spec.and((root, query, cb) -> {
                var late = cb.and(cb.lessThan(root.<Instant>get("dueAt"), now),
                    root.get("status").in(TicketStatus.NEW, TicketStatus.IN_PROGRESS));
                return overdue ? late : cb.not(late);
            });
        }
        return tickets.findAll(spec, pageable).map(TicketResponse::from);
    }
}
