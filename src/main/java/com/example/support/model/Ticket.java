package com.example.support.model;

import jakarta.persistence.*;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false) private Category category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TicketStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "due_at") private Instant dueAt;
    @Column(name = "closed_at") private Instant closedAt;
    protected Ticket() {}
    public Ticket(String title, String description, Category category, Priority priority, Instant dueAt, Clock clock) {
        this.title = title; this.description = description; this.category = category;
        this.priority = priority; this.status = TicketStatus.NEW; this.dueAt = dueAt;
        this.createdAt = clock.instant();
    }
    public void update(String description, Priority priority, Instant dueAt) {
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CANCELLED)
            throw new IllegalStateException("Closed or cancelled ticket cannot be edited");
        this.description = description; this.priority = priority; this.dueAt = dueAt;
    }
    public void changeStatus(TicketStatus target, Clock clock) {
        boolean allowed = switch (status) {
            case NEW -> target == TicketStatus.IN_PROGRESS || target == TicketStatus.CANCELLED;
            case IN_PROGRESS -> target == TicketStatus.RESOLVED || target == TicketStatus.CANCELLED;
            default -> false;
        };
        if (!allowed) throw new IllegalStateException("Forbidden status transition");
        status = target;
        if (target == TicketStatus.RESOLVED) closedAt = clock.instant();
    }
    public boolean isOverdue(Clock clock) {
        return dueAt != null && dueAt.isBefore(clock.instant()) &&
            (status == TicketStatus.NEW || status == TicketStatus.IN_PROGRESS);
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDueAt() { return dueAt; }
    public Instant getClosedAt() { return closedAt; }
}
