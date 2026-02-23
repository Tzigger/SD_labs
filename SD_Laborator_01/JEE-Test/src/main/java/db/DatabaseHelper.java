package db;

import java.sql.*;

public class DatabaseHelper {

    private static final String DB_PATH = System.getProperty("user.home") + "/SD/date_lab/studenti.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        initTable(conn);
        return conn;
    }

    private static void initTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS studenti (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "nume TEXT NOT NULL," +
                     "prenume TEXT NOT NULL," +
                     "varsta INTEGER NOT NULL)";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
}
