package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicienDayDTO {
    private String date;
    private double resolutionTime;
    private long   total;
}