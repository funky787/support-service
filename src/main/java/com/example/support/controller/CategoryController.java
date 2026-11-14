package com.example.support.controller;
import com.example.support.dto.CategoryResponse;
import com.example.support.service.TicketService;

import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/categories")
public class CategoryController {
    private final TicketService service;
    public CategoryController(TicketService service) { this.service = service; }
    @GetMapping public List<CategoryResponse> list() { return service.categories(); }
}
