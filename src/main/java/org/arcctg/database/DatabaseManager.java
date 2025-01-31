package org.arcctg.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.stereotype.Component;

@Component
public class DatabaseManager {
    private static final String JDBC_URL = "jdbc:h2:./data/bot";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection connection = getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255), emoji VARCHAR(255))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
