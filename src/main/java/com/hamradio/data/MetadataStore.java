package com.hamradio.data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MetadataStore {

    private final DatabaseManager db;

    public MetadataStore(DatabaseManager db) {
        this.db = db;
    }

    public long saveScenario(String name, String configJson, String state) throws SQLException {
        String sql = "INSERT INTO scenarios (name, config, state) VALUES (?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, configJson);
            ps.setString(3, state);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    public long saveRecording(long scenarioId, String filePath, double sampleRate,
                              double frequency, long samplesCount) throws SQLException {
        String sql = "INSERT INTO recordings (scenario_id, file_path, sample_rate, frequency, samples_count) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, scenarioId);
            ps.setString(2, filePath);
            ps.setDouble(3, sampleRate);
            ps.setDouble(4, frequency);
            ps.setLong(5, samplesCount);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    public List<String> listScenarios() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM scenarios ORDER BY created_at DESC")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }
}
