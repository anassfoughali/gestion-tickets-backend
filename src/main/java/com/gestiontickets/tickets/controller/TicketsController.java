package com.gestiontickets.tickets.controller;

import com.gestiontickets.tickets.service.TicketsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TicketsController {

    private final TicketsService ticketsService;

    // Liste tous les tickets
    @GetMapping
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(ticketsService.getAllTickets());
    }

    // Stats par jour pour le BarChart
    @GetMapping("/stats-par-jour")
    public ResponseEntity<?> getStatsParJour() {
        return ResponseEntity.ok(ticketsService.getStatsParJour());
    }
}