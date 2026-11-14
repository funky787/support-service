package com.example.support.dto;
import com.example.support.model.TicketStatus;

import jakarta.validation.constraints.NotNull;
public record StatusRequest(@NotNull TicketStatus status) {}
