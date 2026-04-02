package com.gestiontickets.tickets.controller;

import com.gestiontickets.tickets.dto.*;
import com.gestiontickets.tickets.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    //  Garder les SSE emitters actifs
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    //  REST — snapshot instantané
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    //  SSE — stream temps réel (pour frontend sans WebSocket)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStats() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        // Envoyer les données actuelles immédiatement
        try {
            emitter.send(SseEmitter.event()
                    .name("dashboard")
                    .data(dashboardService.getDashboardStats()));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    //  Appelé par le scheduler pour broadcaster via SSE
    public void broadcastToSseClients(DashboardStatsDTO stats) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("dashboard")
                        .data(stats));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }
}