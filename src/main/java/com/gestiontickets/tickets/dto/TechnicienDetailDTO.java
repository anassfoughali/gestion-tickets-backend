package com.gestiontickets.tickets.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicienDetailDTO {
    private String  name;
    private long    totalTickets;
    private long    resolvedTickets;
    private long   cloturedTickets;
    private long    openTickets;
    private double  avgResolutionTime;
    private List<TechnicienDayDTO> parJour;
}