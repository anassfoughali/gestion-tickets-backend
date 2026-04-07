package com.gestiontickets.tickets.service;

import com.gestiontickets.tickets.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TicketsService {

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    // ✅ Constantes au niveau de la CLASSE (pas dans une méthode)
    private static final String SQL_RESOLU =
            "LOWER(s.\"Matchcode\") LIKE '%%resolu%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%résolu%%'";

    private static final String SQL_CLOTURE =
            "LOWER(s.\"Matchcode\") LIKE '%%clôturé%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%cloturé%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%ferm%%'";

    private static final String SQL_OUVERT =
            "LOWER(s.\"Matchcode\") LIKE '%%ouvert%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%nouveau%%'";

    private static final String SQL_EN_COURS =
            "LOWER(s.\"Matchcode\") LIKE '%%cours%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%attente%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%escalad%%' " +
                    "OR LOWER(s.\"Matchcode\") LIKE '%%affect%%'";

    public TicketsService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    public List<TicketFullDTO> getAllTickets() {
        String sql = String.format(
                "SELECT " +
                        "  i.\"IssueID\", " +
                        "  i.\"BriefDescription\", " +
                        "  i.\"CardCode\" as cardName, " +
                        "  g.\"Description\" as technicien, " +
                        "  sType.\"Matchcode\"   as issueType, " +
                        "  sPrio.\"Matchcode\"   as priorite, " +
                        "  sStatut.\"Matchcode\" as statut, " +
                        "  TO_VARCHAR(i.\"RequestDate\", 'YYYY-MM-DD') as requestDate, " +
                        "  TO_VARCHAR(i.\"USER_DateCloture\", 'YYYY-MM-DD') as dateCloture, " +
                        "  CASE WHEN i.\"USER_DateCloture\" IS NOT NULL " +
                        "       AND i.\"USER_DateCloture\" >= i.\"RequestDate\" " +
                        "  THEN ROUND(DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0, 1) " +
                        "  ELSE NULL END as duree " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "LEFT JOIN \"%s\".\"MARISupportGroup\" g " +
                        "  ON i.\"SupportGroupID\" = g.\"GroupId\" " +
                        "LEFT JOIN \"%s\".\"MARISupportSettings\" sStatut " +
                        "  ON i.\"Status\" = sStatut.\"ID\" AND sStatut.\"Setting\" = 1 " +
                        "LEFT JOIN \"%s\".\"MARISupportSettings\" sType " +
                        "  ON i.\"IssueType\" = sType.\"ID\" AND sType.\"Setting\" = 2 " +
                        "LEFT JOIN \"%s\".\"MARISupportSettings\" sPrio " +
                        "  ON i.\"Priority\" = sPrio.\"ID\" AND sPrio.\"Setting\" = 3 " +
                        "ORDER BY i.\"RequestDate\" DESC",
                schema, schema, schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> TicketFullDTO.builder()
                .issueID(rs.getString("IssueID"))
                .briefDescription(rs.getString("BriefDescription"))
                .cardName(rs.getString("cardName"))
                .technicien(rs.getString("technicien"))
                .issueType(rs.getString("issueType"))
                .priority(rs.getString("priorite"))
                .status(rs.getString("statut"))
                .requestDate(rs.getString("requestDate"))
                .dateCloture(rs.getString("dateCloture"))
                .duree(rs.getObject("duree") != null ? rs.getDouble("duree") : null)
                .build()
        );
    }

    public List<StatJourDTO> getStatsParJour() {
        String sql = String.format(
                "SELECT " +
                        "  TO_VARCHAR(i.\"RequestDate\", 'DD/MM') as jour, " +
                        "  COUNT(*) as total, " +
                        "  COUNT(CASE WHEN " + SQL_RESOLU   + " THEN 1 END) as resolus, " +
                        "  COUNT(CASE WHEN " + SQL_CLOTURE  + " THEN 1 END) as clotures, " +
                        "  COUNT(CASE WHEN " + SQL_OUVERT   + " THEN 1 END) as ouverts, " +
                        "  COUNT(CASE WHEN " + SQL_EN_COURS + " THEN 1 END) as enCours " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "WHERE i.\"RequestDate\" >= ADD_DAYS(CURRENT_DATE, -60) " +
                        "GROUP BY TO_VARCHAR(i.\"RequestDate\", 'DD/MM'), i.\"RequestDate\" " +
                        "ORDER BY i.\"RequestDate\" ASC",
                schema, schema);

        // ✅ Ordre exact : date, resolved, clotures, open, enCours, total
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StatJourDTO(
                rs.getString("jour"),
                rs.getLong("resolus"),
                rs.getLong("clotures"),
                rs.getLong("ouverts"),
                rs.getLong("enCours"),
                rs.getLong("total")
        ));
    }
}