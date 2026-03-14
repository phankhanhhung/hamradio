package com.hamradio.data;

import java.sql.*;

public class DatabaseManager {

    private Connection connection;
    private final String dbPath;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
    }

    public void initialize() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createSchema();
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS scenarios (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name TEXT NOT NULL," +
                "  config TEXT," +
                "  state TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS recordings (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  scenario_id INTEGER," +
                "  file_path TEXT NOT NULL," +
                "  sample_rate REAL," +
                "  frequency REAL," +
                "  samples_count INTEGER," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (scenario_id) REFERENCES scenarios(id)" +
                ")"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS stations (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  scenario_id INTEGER," +
                "  callsign TEXT NOT NULL," +
                "  latitude REAL," +
                "  longitude REAL," +
                "  frequency REAL," +
                "  mode TEXT," +
                "  FOREIGN KEY (scenario_id) REFERENCES scenarios(id)" +
                ")"
            );
        }
    }

    public Connection getConnection() { return connection; }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) { }
        }
    }
}
