package com.gestiontickets.tickets.service;

import com.gestiontickets.tickets.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TechnicienService {

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    public TechnicienService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    // ✅ Liste tous les techniciens
    public List<TechnicienDTO> getAllTechniciens() {
        String sql = String.format(
                "SELECT \"GroupId\", \"Description\" " +
                        "FROM \"%s\".\"MARISupportGroup\" " +
                        "ORDER BY \"Description\" ASC", schema);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new TechnicienDTO(
                        rs.getString("GroupId"),
                        rs.getString("Description")
                )
        );
    }

    // ✅ Stats globales tous les techniciens
    public List<TechnicienStatGlobaleDTO> getStatsGlobales() {
        String sql = String.format(
                "SELECT " +
                        "  g.\"Description\" as technicien, " +
                        "  COUNT(*) as total, " +
                        "  COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%clot%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%ferm%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%sans solution%%' " +
                        "             THEN 1 END) as resolus, " +
                        "  COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%ouvert%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%nouveau%%' " +
                        "             THEN 1 END) as ouverts, " +
                        "  COALESCE(AVG(CASE WHEN i.\"USER_DateCloture\" IS NOT NULL " +
                        "    AND i.\"USER_DateCloture\" >= i.\"RequestDate\" " +
                        "    THEN DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0 END), 0) as avgRes " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportGroup\" g ON i.\"SupportGroupID\" = g.\"GroupId\" " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "GROUP BY g.\"Description\" " +
                        "ORDER BY resolus DESC",
                schema, schema, schema);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new TechnicienStatGlobaleDTO(
                        rs.getString("technicien"),
                        rs.getLong("total"),
                        rs.getLong("resolus"),
                        rs.getLong("ouverts"),
                        rs.getDouble("avgRes")
                )
        );
    }

    // ✅ Détail d'un technicien par groupId
    public TechnicienDetailDTO getTechnicienStats(String groupId) {

        // Nom
        String sqlName = String.format(
                "SELECT \"Description\" FROM \"%s\".\"MARISupportGroup\" WHERE \"GroupId\" = ?", schema);
        String name = jdbcTemplate.queryForObject(sqlName, String.class, groupId);

        // KPIs
        String sqlKpi = String.format(
                "SELECT " +
                        "  COUNT(*) as total, " +
                        "  COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%clot%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%ferm%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%sans solution%%' " +
                        "             THEN 1 END) as resolus, " +
                        "  COUNT(CASE WHEN LOWER(s.\"Matchcode\") LIKE '%%ouvert%%' " +
                        "              OR LOWER(s.\"Matchcode\") LIKE '%%nouveau%%' " +
                        "             THEN 1 END) as ouverts, " +
                        "  COALESCE(AVG(CASE WHEN i.\"USER_DateCloture\" IS NOT NULL " +
                        "    AND i.\"USER_DateCloture\" >= i.\"RequestDate\" " +
                        "    THEN DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0 END), 0) as avgRes " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "JOIN \"%s\".\"MARISupportSettings\" s " +
                        "  ON i.\"Status\" = s.\"ID\" AND s.\"Setting\" = 1 " +
                        "WHERE i.\"SupportGroupID\" = ?",
                schema, schema);

        var kpi = jdbcTemplate.queryForMap(sqlKpi, groupId);

        // Évolution par jour (30 derniers jours)
        String sqlJour = String.format(
                "SELECT " +
                        "  TO_VARCHAR(i.\"RequestDate\", 'DD/MM') as jour, " +
                        "  COUNT(*) as total, " +
                        "  COALESCE(AVG(CASE WHEN i.\"USER_DateCloture\" IS NOT NULL " +
                        "    AND i.\"USER_DateCloture\" >= i.\"RequestDate\" " +
                        "    THEN DAYS_BETWEEN(i.\"RequestDate\", i.\"USER_DateCloture\") * 24.0 END), 0) as avgRes " +
                        "FROM \"%s\".\"MARISupportIssue\" i " +
                        "WHERE i.\"SupportGroupID\" = ? " +
                        "  AND i.\"RequestDate\" >= ADD_DAYS(CURRENT_DATE, -30) " +
                        "GROUP BY TO_VARCHAR(i.\"RequestDate\", 'DD/MM'), i.\"RequestDate\" " +
                        "ORDER BY i.\"RequestDate\" ASC", schema);

        List<TechnicienDayDTO> parJour = jdbcTemplate.query(sqlJour,
                (rs, rowNum) -> new TechnicienDayDTO(
                        rs.getString("jour"),
                        rs.getDouble("avgRes"),
                        rs.getLong("total")
                ), groupId);

        return TechnicienDetailDTO.builder()
                .name(name)
                .totalTickets(((Number) kpi.get("total")).longValue())
                .resolvedTickets(((Number) kpi.get("resolus")).longValue())
                .openTickets(((Number) kpi.get("ouverts")).longValue())
                .avgResolutionTime(((Number) kpi.get("avgRes")).doubleValue())
                .parJour(parJour)
                .build();
    }
}