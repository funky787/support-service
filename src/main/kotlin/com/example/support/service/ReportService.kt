package com.example.support.service

import com.example.support.dto.SummaryResponse
import com.example.support.model.TicketStatus
import com.example.support.repository.TicketRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class ReportService(private val tickets: TicketRepository, private val clock: Clock) {
    @Transactional(readOnly = true)
    fun summary(
        from: Instant,
        to: Instant,
    ): SummaryResponse {
        if (!from.isBefore(to)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report period")
        val rows = tickets.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)
        val resolvedSeconds =
            rows.asSequence()
                .filter { it.status == TicketStatus.RESOLVED }
                .mapNotNull { ticket -> ticket.closedAt?.let { Duration.between(ticket.createdAt, it).toMillis() / 1000.0 } }
                .toList()
        return SummaryResponse(
            rows.size,
            rows.groupingBy { it.status }.eachCount(),
            rows.groupingBy { it.priority }.eachCount(),
            rows.count { it.isOverdue(clock) },
            resolvedSeconds.takeIf { it.isNotEmpty() }?.average(),
        )
    }
}
