package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicienStatGlobaleDTO {
    private String name;
    private long   total;
    private long   resolus;
    private long   cloture;
    private long   ouverts;
    private double avgResolutionTime;
}