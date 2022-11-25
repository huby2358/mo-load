package io.mo.conn;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionOperation {

    public static Connection getConnection() {
        DatabaseConnection databaseConnection = new MOConnection();
        return databaseConnection.BuildDatabaseConnection();
    }

    public static void CloseConnection(Connection connection) throws SQLException {
        connection.close();
    }
}
