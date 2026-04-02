package com.gestiontickets.tickets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TicketFullDTO {
    private String issueID;
    private String briefDescription;
    private String cardName;
    private String technicien;
    private String issueType;
    private String priority;
    private String status;
    private String requestDate;
    private String dateCloture;
    private Double duree;
}
