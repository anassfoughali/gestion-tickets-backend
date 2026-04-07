package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatJourDTO {
    private String date;
    private long resolved;
    private long clotures;
    private long open;
    private long enCours;
    private long total;
}