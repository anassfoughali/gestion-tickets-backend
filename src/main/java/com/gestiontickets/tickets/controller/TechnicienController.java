package com.gestiontickets.tickets.controller;

import com.gestiontickets.tickets.service.TechnicienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/techniciens")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TechnicienController {

    private final TechnicienService technicienService;

    @GetMapping
    public ResponseEntity<?> getAllTechniciens() {
        return ResponseEntity.ok(technicienService.getAllTechniciens());
    }

    @GetMapping("/stats-globales")
    public ResponseEntity<?> getStatsGlobales() {
        return ResponseEntity.ok(technicienService.getStatsGlobales());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getTechnicienStats(@PathVariable String groupId) {
        return ResponseEntity.ok(technicienService.getTechnicienStats(groupId));
    }
}