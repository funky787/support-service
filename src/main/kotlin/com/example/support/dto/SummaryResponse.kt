package com.example.support.dto

import com.example.support.model.Priority
import com.example.support.model.TicketStatus

data class SummaryResponse(
    val created: Int,
    val byStatus: Map<TicketStatus, Int>,
    val byPriority: Map<Priority, Int>,
    val overdueOpen: Int,
    val averageResolutionSeconds: Double?,
)
