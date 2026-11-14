package com.example.support.controller;
import com.example.support.dto.SummaryResponse;
import com.example.support.service.ReportService;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service = service; }
    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam Instant from, @RequestParam Instant to) {
        return service.summary(from, to);
    }
}
