package io.mo.conn;

import io.mo.CONFIG;
import io.mo.thread.PrepareStatmentExecutor;
import io.mo.util.MoConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MOConnection implements DatabaseConnection {
    private static String conn = MoConfUtil.getURL();
    private static String database = MoConfUtil.getDefaultDatabase();
    private static String username = MoConfUtil.getUserName();
    private static String password = MoConfUtil.getUserpwd();
    private static String driver = MoConfUtil.getDriver();

    private static Logger LOG = Logger.getLogger(MOConnection.class.getName());

    @Override
    public Connection BuildDatabaseConnection() {
        conn = MoConfUtil.getURL();
        if(CONFIG.SPEC_USERNAME != null)
            username = CONFIG.SPEC_USERNAME;

        if(CONFIG.SPEC_PASSWORD != null)
            password = CONFIG.SPEC_PASSWORD;
        try {
            Class.forName(driver);
            Connection connection = DriverManager.getConnection(conn,username,password);
            return connection;
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            LOG.error(String.format("Connection Info : username=%s,password=%s,jdbcURL=%s",username,password,conn));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
