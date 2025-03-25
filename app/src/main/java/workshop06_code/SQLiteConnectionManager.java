package workshop06_code;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class SQLiteConnectionManager {
    private static final Logger logger = Logger.getLogger(SQLiteConnectionManager.class.getName());

    static {
        try {
            InputStream is = SQLiteConnectionManager.class.getClassLoader().getResourceAsStream("logging.properties");
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                logger.info("Logging configured successfully in SQLiteConnectionManager.");
            } else {
                logger.severe("Could not find logging.properties on classpath");
            }
        } catch (SecurityException | IOException e) {
            logger.log(Level.SEVERE, "Failed to configure logging: " + e.getMessage(), e);
        }
    }

    private String databaseURL;

    private static final String WORDLE_DROP_TABLE_STRING = "DROP TABLE IF EXISTS wordlist";
    private static final String WORDLE_CREATE_STRING = "CREATE TABLE wordlist (id INTEGER PRIMARY KEY, word TEXT NOT NULL)";
    private static final String VALID_WORDS_DROP_TABLE_STRING = "DROP TABLE IF EXISTS validWords";
    private static final String VALID_WORDS_CREATE_STRING = "CREATE TABLE validWords (id INTEGER PRIMARY KEY, word TEXT NOT NULL)";

    public SQLiteConnectionManager(String filename) {
        databaseURL = "jdbc:sqlite:" + filename;  // Simplified path to avoid sqlite/ dir issue
    }

    public void createNewDatabase(String fileName) {
        try (Connection conn = DriverManager.getConnection(databaseURL)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                logger.info("Database driver: " + meta.getDriverName());
                logger.info("New database created at " + databaseURL);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create database: " + e.getMessage(), e);
        }
    }

    public boolean checkIfConnectionDefined() {
        if (databaseURL == null || databaseURL.isEmpty()) {
            logger.severe("Database URL is not set.");
            return false;
        }
        try (Connection conn = DriverManager.getConnection(databaseURL)) {
            return conn != null;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Connection check failed: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean createWordleTables() {
        if (databaseURL == null || databaseURL.isEmpty()) {
            logger.severe("Database URL is not set.");
            return false;
        }
        try (Connection conn = DriverManager.getConnection(databaseURL)) {
            try (PreparedStatement stmt = conn.prepareStatement(WORDLE_DROP_TABLE_STRING)) {
                stmt.execute();
                logger.info("Dropped wordlist table if it existed.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(WORDLE_CREATE_STRING)) {
                stmt.execute();
                logger.info("Created wordlist table.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(VALID_WORDS_DROP_TABLE_STRING)) {
                stmt.execute();
                logger.info("Dropped validWords table if it existed.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(VALID_WORDS_CREATE_STRING)) {
                stmt.execute();
                logger.info("Created validWords table.");
            }
            logger.info("Wordle tables created successfully.");
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create tables: " + e.getMessage(), e);
            return false;
        }
    }

    public void addValidWord(int id, String word) {
        String sql = "INSERT INTO validWords(id, word) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(databaseURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, word);
            pstmt.executeUpdate();
            logger.fine("Added word '" + word + "' with id " + id);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add word '" + word + "': " + e.getMessage(), e);
        }
    }

    public boolean isValidWord(String guess) {
        String sql = "SELECT COUNT(id) AS total FROM validWords WHERE word LIKE ?";
        try (Connection conn = DriverManager.getConnection(databaseURL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, guess);
            ResultSet resultRows = stmt.executeQuery();
            if (resultRows.next()) {
                int result = resultRows.getInt("total");
                return result >= 1;
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to check word '" + guess + "': " + e.getMessage(), e);
            return false;
        }
    }
}