package io.mo.thread;

import io.mo.CONFIG;
import io.mo.DTest;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

public class TestExecutor implements Runnable{
    
    private Connection connection = null;
    private int id = 0;
    private String sql = null;
    private Statement statement = null;
    private static Logger LOG = Logger.getLogger(TestExecutor.class.getName());
    
    public TestExecutor(Connection connection, int id){
        this.connection = connection;
        this.id = id;
        int tbl = id%10 + 1;
        sql = String.format("select k from sbtest%s where id = 10000;",tbl);
    }
    
    @Override
    public void run() {
        try {
            statement = connection.createStatement();
            LOG.info(String.format("Thread[%d] start to run.........",id));
            while(!CONFIG.TIMEOUT){
                statement.execute(sql);
            }
            LOG.info(String.format("Thread[%d] has finished the test",id));
        } catch (SQLException e) {
            LOG.error(String.format("Exit. Cause: " + e.getMessage()));
            System.exit(1);
        }
    }
}
