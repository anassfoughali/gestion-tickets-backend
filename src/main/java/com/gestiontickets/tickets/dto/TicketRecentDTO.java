package com.gestiontickets.tickets.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRecentDTO {
    private String issueID;
    private String briefDescription;
    private String technicien;
    private String priority;
    private String status;
    private String requestDate;
    private String cardName;
}