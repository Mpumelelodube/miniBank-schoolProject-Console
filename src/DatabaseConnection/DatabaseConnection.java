package DatabaseConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String SQLITE_CONN = "jdbc:sqlite:bank.sqlite";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");

            System.out.println("connected");

            return DriverManager.getConnection(SQLITE_CONN);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Not connected");
        return null;
    }
}
