package io.mo.conn;

import io.mo.util.MoConfUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MOConnection implements DatabaseConnection {
    private static String conn = MoConfUtil.getURL();
    private static String database = MoConfUtil.getDefaultDatabase();
    private static String username = MoConfUtil.getUserName();
    private static String password = MoConfUtil.getUserpwd();
    private static String driver = MoConfUtil.getDriver();

    @Override
    public Connection BuildDatabaseConnection() {

        for(int i = 0; i < 3; i++) {
            try {
                Class.forName(driver);
                Connection connection = DriverManager.getConnection(conn,username,password);
                return connection;
            } catch (SQLException e) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return null;
    }
}
