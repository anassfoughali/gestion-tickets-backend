package com.gestiontickets.tickets.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private KpiDTO kpi;
    private List<StatJourDTO> parJour;
    private List<TechnicienStatDTO> parTechnicien;
    private List<TicketRecentDTO> ticketsRecents;
    private long lastUpdated;  //  timestamp du dernier refresh
}