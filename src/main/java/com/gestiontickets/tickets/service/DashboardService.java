package com.gestiontickets.tickets.service;

import com.gestiontickets.tickets.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final String schema;

    private final AtomicReference<DashboardStatsDTO> cachedStats = new AtomicReference<>();

    public DashboardService(
            JdbcTemplate jdbcTemplate,
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.messagingTemplate = messagingTemplate;
        this.schema = schema;
    }

    @Scheduled(fixedRateString = "${app.refresh-interval:30000}")
    public void refreshAndBroadcast() {
        try {
            DashboardStatsDTO stats = fetchFromDatabase();
            cachedStats.set(stats);
            messagingTemplate.convertAndSend("/topic/dashboard", stats);
            System.out.println("✅ Dashboard broadcasted at " + new Date());
        } catch (Exception e) {
            System.err.println("❌ Erreur refresh : " + e.getMessage());
        }
    }

    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = cachedStats.get();
        if (stats == null) {
            stats = fetchFromDatabase();
            cachedStats.set(stats);
        }
        return stats;
    }

    private DashboardStatsDTO fetchFromDatabase() {
        return DashboardStatsDTO.builder()
                .kpi(getKpi())
                .parJour(getStatsParJour())
                .parTechnicien(getStatsParTechnicien())
                .ticketsRecents(getTicketsRecents())
                .lastUpdated(System.currentTimeMillis())
                .build();
    }

    public KpiDTO getKpi() {
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"" + schema + "\".\"MARISupportIssue\"",
                Long.class
        );

        //  Setting = 1 pour les STATUTS
        String sqlStatuts = String.format(
                "SELECT s.\"Matchcode\", COUNT(i.\"IssueID\") as cnt " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "GROUP BY s.\"Matchcode\"", schema, schema);

        Map<String, Long> statutCounts = new HashMap<>();
        jdbcTemplate.query(sqlStatuts, (RowCallbackHandler) rs -> {
            String matchcode = rs.getString("Matchcode");
            long cnt = rs.getLong("cnt");
            statutCounts.put(matchcode, cnt);
            System.out.println("📊 Statut: [" + matchcode + "] = " + cnt);
        });

        long resolus = 0, enCours = 0, ouverts = 0;
        for (Map.Entry<String, Long> entry : statutCounts.entrySet()) {
            String key = entry.getKey().toLowerCase().trim();
            long val = entry.getValue();

            // Mapping exact avec les vrais Matchcode SAP HANA
            if (key.contains("clot") || key.contains("ferm") ||
                    key.contains("sans solution") || key.contains("resolu") ||
                    key.contains("résolu") || key.contains("termin")) {
                resolus += val;
            } else if (key.contains("cours") || key.contains("progress") ||
                    key.contains("traitement") || key.contains("affect") ||
                    key.contains("assign")) {
                enCours += val;
            } else if (key.contains("ouvert") || key.contains("open") ||
                    key.contains("nouveau") || key.contains("new")) {
                ouverts += val;
            } else {
                // Tout le reste = ouvert
                ouverts += val;
            }
        }

        // AVG uniquement dates cohérentes
        String sqlAvg = String.format(
                "SELECT AVG(DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0) " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "WHERE i.\"USER_DateCloture\" IS NOT NULL " +
                        "AND i.\"USER_DateCloture\" >= i.\"RequestDate\"", schema);

        String sqlSla = String.format(
                "SELECT COUNT(CASE WHEN \"USER_DateCloture\" IS NOT NULL THEN 1 END) * 100.0 / COUNT(*) " +
                        "FROM \"%s\".\"MARISupportIssue\"", schema);

        Double avgH = jdbcTemplate.queryForObject(sqlAvg, Double.class);
        Double sla  = jdbcTemplate.queryForObject(sqlSla, Double.class);

        return KpiDTO.builder()
                .totalTickets(total)
                .openTickets(ouverts)
                .resolvedTickets(resolus)
                .inProgressTickets(enCours)
                .avgResolutionTime(avgH != null ? String.format("%.1fh", avgH) : "N/A")
                .slaCompliance(sla != null ? String.format("%.0f%%", sla) : "N/A")
                .build();
    }

    public List<StatJourDTO> getStatsParJour() {
        // Setting = 1 pour statuts + 30 jours
        String sql = String.format(
                "SELECT " +
                        "    TO_VARCHAR(i.\"RequestDate\", 'DD/MM') as jour, " +
                        "    COUNT(*) as total, " +
                        "    COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%clot%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%ferm%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%sans solution%%' " +
                        "               THEN 1 END) as resolus, " +
                        "    COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%ouvert%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%open%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%nouveau%%' " +
                        "               THEN 1 END) as ouverts, " +
                        "    COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%cours%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%affect%%' " +
                        "               THEN 1 END) as enCours " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "WHERE i.\"RequestDate\" >= ADD_DAYS(CURRENT_DATE, -30) " +
                        "GROUP BY TO_VARCHAR(i.\"RequestDate\", 'DD/MM'), i.\"RequestDate\" " +
                        "ORDER BY i.\"RequestDate\" ASC", schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StatJourDTO(
                rs.getString("jour"),
                rs.getLong("resolus"),
                rs.getLong("ouverts"),
                rs.getLong("enCours"),
                rs.getLong("total")
        ));
    }

    public List<TechnicienStatDTO> getStatsParTechnicien() {
        // Setting = 1 pour statuts
        String sql = String.format(
                "SELECT " +
                        "    g.\"Description\" as technicien, " +
                        "    COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%clot%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%ferm%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%sans solution%%' " +
                        "               THEN 1 END) as resolus, " +
                        "    COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%ouvert%%' " +
                        "                 OR LOWER(s.\"Matchcode\") LIKE '%%nouveau%%' " +
                        "               THEN 1 END) as ouverts, " +
                        "    COALESCE(AVG(" +
                        "        CASE WHEN i.\"USER_DateCloture\" IS NOT NULL " +
                        "             AND i.\"USER_DateCloture\" >= i.\"RequestDate\" " +
                        "        THEN DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0 " +
                        "        END" +
                        "    ), 0) as avgResolution " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportGroup\" g " +
                        "  ON i.\"SupportGroupID\" = g.\"GroupId\" " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "GROUP BY g.\"Description\" " +
                        "ORDER BY resolus DESC", schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TechnicienStatDTO(
                rs.getString("technicien"),
                rs.getLong("resolus"),
                rs.getLong("ouverts"),
                rs.getDouble("avgResolution")
        ));
    }

    public List<TicketRecentDTO> getTicketsRecents() {
        String sql = String.format(
                "SELECT TOP 6 " +
                        "    i.\"IssueID\", " +
                        "    i.\"BriefDescription\", " +
                        "    g.\"Description\"     as technicien, " +
                        "    sStatut.\"Matchcode\" as statut, " +
                        "    sPrio.\"Matchcode\"   as priorite, " +
                        "    TO_VARCHAR(i.\"RequestDate\", 'YYYY-MM-DD') as requestDate, " +
                        "    i.\"CardCode\"        as cardName " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "LEFT JOIN \"%s\".\"MARISupportGroup\" g " +
                        "    ON i.\"SupportGroupID\" = g.\"GroupId\" " +
                        // Setting = 1 pour statut
                        "LEFT JOIN \"%s\".\"MARISupportSettings\" sStatut " +
                        "    ON i.\"Status\" = sStatut.\"ID\" AND sStatut.\"Setting\" = 1 " +
                        // Setting = 3 pour priorité
                        "LEFT JOIN \"%s\".\"MARISupportSettings\" sPrio " +
                        "    ON i.\"Priority\" = sPrio.\"ID\" AND sPrio.\"Setting\" = 3 " +
                        "ORDER BY i.\"RequestDate\" DESC", schema, schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) -> TicketRecentDTO.builder()
                .issueID(rs.getString("IssueID"))
                .briefDescription(rs.getString("BriefDescription"))
                .technicien(rs.getString("technicien"))
                .status(rs.getString("statut"))
                .priority(rs.getString("priorite"))
                .requestDate(rs.getString("requestDate"))
                .cardName(rs.getString("cardName"))
                .build()
        );
    }
}