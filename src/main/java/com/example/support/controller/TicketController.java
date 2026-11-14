package com.example.support.controller;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.EditTicketRequest;
import com.example.support.dto.HistoryResponse;
import com.example.support.dto.StatusRequest;
import com.example.support.dto.TicketResponse;
import com.example.support.model.Priority;
import com.example.support.model.TicketStatus;
import com.example.support.service.TicketService;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;
    public TicketController(TicketService service) { this.service = service; }
    @PostMapping public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + created.id())).body(created);
    }
    @GetMapping("/{id}") public TicketResponse get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/{id}") public TicketResponse edit(@PathVariable Long id, @Valid @RequestBody EditTicketRequest request) {
        return service.edit(id, request);
    }
    @PatchMapping("/{id}/status") public TicketResponse changeStatus(@PathVariable Long id,
        @Valid @RequestBody StatusRequest request) { return service.changeStatus(id, request); }
    @GetMapping("/{id}/history") public List<HistoryResponse> history(@PathVariable Long id) { return service.history(id); }
    @GetMapping public Page<TicketResponse> list(@RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) Priority priority, @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Boolean overdue,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.list(status, priority, categoryId, overdue, pageable);
    }
}
