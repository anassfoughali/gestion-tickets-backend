package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDTO {
    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private long cloturedTickets;
    private long inProgressTickets;
    private String avgResolutionTime;
    private String slaCompliance;
}