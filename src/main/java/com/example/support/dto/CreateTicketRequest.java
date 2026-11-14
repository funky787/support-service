package com.example.support.dto;
import com.example.support.model.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTicketRequest(
    @NotBlank @Size(max = 200) String title,
    String description,
    @NotNull Long categoryId,
    Priority priority,
    Instant dueAt
) {}
