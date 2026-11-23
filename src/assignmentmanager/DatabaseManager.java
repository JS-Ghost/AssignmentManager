package assignmentmanager;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles all MySQL JDBC database operations and configuration.
 * Gracefully handles offline mode if MySQL is not reachable.
 */
public class DatabaseManager {

    private static final String CONFIG_FILE = "db.properties";
    private static DatabaseManager instance;

    private String host = "localhost";
    private int port = 3306;
    private String dbName = "assignment_db";
    private String username = "root";
    private String password = "";

    private Connection connection;
    private boolean connected = false;
    private String lastError = null;

    private DatabaseManager() {
        loadConfig();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void loadConfig() {
        File file = new File(CONFIG_FILE);
        Properties props = new Properties();
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                host = props.getProperty("db.host", "localhost").trim();
                port = Integer.parseInt(props.getProperty("db.port", "3306").trim());
                dbName = props.getProperty("db.name", "assignment_db").trim();
                username = props.getProperty("db.username", "root").trim();
                password = props.getProperty("db.password", "");
            } catch (Exception e) {
                System.err.println("Warning: Failed to load " + CONFIG_FILE + ": " + e.getMessage());
            }
        } else {
            saveConfig(host, port, dbName, username, password);
        }
    }

    public boolean saveConfig(String host, int port, String dbName, String username, String password) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", String.valueOf(port));
        props.setProperty("db.name", dbName);
        props.setProperty("db.username", username);
        props.setProperty("db.password", password != null ? password : "");

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Assignment Manager Database Configuration");
            return true;
        } catch (IOException e) {
            lastError = "Failed to save configuration: " + e.getMessage();
            return false;
        }
    }

    public synchronized boolean connect() {
        close();
        lastError = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            connected = false;
            lastError = "MySQL JDBC Driver (com.mysql.cj.jdbc.Driver) not found in classpath.";
            System.err.println(lastError);
            return false;
        }

        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

        try {
            connection = DriverManager.getConnection(dbUrl, username, password);
            initSchema(connection);
            connected = true;
            return true;
        } catch (SQLException e) {
            // If unknown database (error code 1049), attempt to create database
            if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().contains("Unknown database"))) {
                try {
                    String rootUrl = "jdbc:mysql://" + host + ":" + port + "/"
                            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
                    try (Connection rootConn = DriverManager.getConnection(rootUrl, username, password);
                         Statement stmt = rootConn.createStatement()) {
                        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName
                                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    }
                    connection = DriverManager.getConnection(dbUrl, username, password);
                    initSchema(connection);
                    connected = true;
                    return true;
                } catch (SQLException ex) {
                    lastError = "Could not auto-create database '" + dbName + "': " + ex.getMessage();
                }
            } else {
                lastError = e.getMessage();
            }
        }

        connected = false;
        return false;
    }

    private void initSchema(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS assignments ("
                + "id INT PRIMARY KEY, "
                + "course_code VARCHAR(50) NOT NULL, "
                + "title VARCHAR(255) NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'Pending', "
                + "due_date VARCHAR(50) DEFAULT '', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public synchronized void close() {
        connected = false;
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
            connection = null;
        }
    }

    public boolean isConnected() {
        if (!connected || connection == null) return false;
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            connected = false;
            return false;
        }
    }

    public String getLastError() {
        return lastError;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDbName() { return dbName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // --- CRUD Operations ---

    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        if (!isConnected()) return list;

        String sql = "SELECT id, course_code, title, status, due_date FROM assignments ORDER BY id ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Assignment(
                        rs.getInt("id"),
                        rs.getString("course_code"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getString("due_date")
                ));
            }
        } catch (SQLException e) {
            lastError = "Error loading assignments: " + e.getMessage();
        }
        return list;
    }

    public boolean addAssignment(Assignment a) throws SQLException {
        if (!isConnected()) return false;
        String sql = "INSERT INTO assignments (id, course_code, title, status, due_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, a.getId());
            ps.setString(2, a.getCourseCode());
            ps.setString(3, a.getTitle());
            ps.setString(4, a.getStatus());
            ps.setString(5, a.getDueDate());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateAssignment(Assignment a) throws SQLException {
        if (!isConnected()) return false;
        String sql = "UPDATE assignments SET course_code = ?, title = ?, status = ?, due_date = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.getCourseCode());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getStatus());
            ps.setString(4, a.getDueDate());
            ps.setInt(5, a.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAssignment(int id) throws SQLException {
        if (!isConnected()) return false;
        String sql = "DELETE FROM assignments WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean clearAllAssignments() throws SQLException {
        if (!isConnected()) return false;
        String sql = "DELETE FROM assignments";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        }
    }

    public boolean isIdExists(int id) {
        if (!isConnected()) return false;
        String sql = "SELECT COUNT(*) FROM assignments WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ignored) {}
        return false;
    }

    public String testConnectionDetails(String tHost, int tPort, String tDb, String tUser, String tPass) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String testUrl = "jdbc:mysql://" + tHost + ":" + tPort + "/" + tDb
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
            try (Connection testConn = DriverManager.getConnection(testUrl, tUser, tPass);
                 Statement stmt = testConn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + tDb + "'")) {
                int count = 0;
                if (rs.next()) count = rs.getInt(1);
                return "SUCCESS: Connected to MySQL (" + tDb + ", " + count + " tables found).";
            }
        } catch (ClassNotFoundException e) {
            return "ERROR: MySQL JDBC Driver not found.";
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
