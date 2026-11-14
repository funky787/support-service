package com.example.support.dto;
import com.example.support.model.Priority;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record EditTicketRequest(String description, @NotNull Priority priority, Instant dueAt) {}
