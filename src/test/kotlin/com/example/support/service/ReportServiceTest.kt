package com.example.support.service

import com.example.support.model.*
import com.example.support.repository.TicketRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReportServiceTest {
    private val clock = Clock.fixed(Instant.parse("2026-01-02T00:00:00Z"), ZoneOffset.UTC)
    private val repo = mock(TicketRepository::class.java)
    private val service = ReportService(repo, clock)
    private val from = Instant.parse("2026-01-01T00:00:00Z")
    private val to = Instant.parse("2026-01-03T00:00:00Z")

    @Test fun emptyRowsHaveNullAverage() {
        `when`(repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)).thenReturn(emptyList())
        val result = service.summary(from, to)
        assertEquals(0, result.created)
        assertNull(result.averageResolutionSeconds)
    }
    @Test fun invalidPeriodIs400() {
        assertEquals(400, assertThrows(ResponseStatusException::class.java) { service.summary(to, from) }.statusCode.value())
        verifyNoInteractions(repo)
    }
    @Test fun javaEntityIsIncludedInStatistics() {
        val javaTicket = Ticket("Printer", null, null, Priority.HIGH, from.minusSeconds(10), clock)
        `when`(repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)).thenReturn(listOf(javaTicket))
        val result = service.summary(from, to)
        assertEquals(1, result.created)
        assertEquals(1, result.byStatus[TicketStatus.NEW])
        assertEquals(1, result.byPriority[Priority.HIGH])
        assertEquals(1, result.overdueOpen)
        assertNull(result.averageResolutionSeconds)
    }
    @Test fun resolvedDurationUsesClosedAt() {
        val t = Ticket("Printer", null, null, Priority.LOW, null, clock)
        t.changeStatus(TicketStatus.IN_PROGRESS, clock)
        t.changeStatus(TicketStatus.RESOLVED, Clock.offset(clock, java.time.Duration.ofHours(2)))
        `when`(repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)).thenReturn(listOf(t))
        assertEquals(7200.0, service.summary(from, to).averageResolutionSeconds)
    }
    @Test fun noResolvedTicketsHaveNullAverage() {
        val t = Ticket("Printer", null, null, Priority.LOW, null, clock)
        `when`(repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to)).thenReturn(listOf(t))
        assertNull(service.summary(from, to).averageResolutionSeconds)
    }
}
