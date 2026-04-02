package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicienStatDTO {
    private String name;
    private long ticketsResolved;
    private long ticketsOpen;
    private double avgResolutionTime;
}