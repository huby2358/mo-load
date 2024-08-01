package io.mo.replace;

import io.mo.CONFIG;
import io.mo.conn.ConnectionOperation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DBResultSetVariable implements Variable {
    private String name;
    private String sql = null;
    private int refresh = 0;
    
    private ArrayList<String> values = new ArrayList<>();
    
    private Connection connection = null;
    private Statement statement = null;
    private boolean inRefresh = false;
    private int size = 0;
    private Random random = new Random();
    private int scope = CONFIG.PARA_SCOPE_TRANSCATION;
    
    public void init(){
        connection = ConnectionOperation.getConnection();
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while(resultSet.next()){
                String value = resultSet.getString(1);
                values.add(value);
            }
            size = values.size();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        if(refresh != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!CONFIG.TIMEOUT) {
                        try {
                            Thread.sleep(refresh * 1000);
                            if (connection != null || !connection.isClosed() || !connection.isValid(1000)) {
                                connection = ConnectionOperation.getConnection();
                                statement = connection.createStatement();
                            }
                            ResultSet resultSet = statement.executeQuery(sql);
                            inRefresh = true;
                            values.clear();
                            while (resultSet.next()) {
                                String value = resultSet.getString(1);
                                values.add(value);
                            }
                            size = values.size();
                            inRefresh = false;

                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }).start();
        }

    }
    
    public String getName(){return this.name;}



    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }


    public DBResultSetVariable(String name, String sql){
        this.name = name;
        this.sql = sql;
    }

    public DBResultSetVariable(String name, String sql, int refresh){
        this.name = name;
        this.sql = sql;
        this.refresh = refresh;
    }

    public synchronized String nextValue(){
        if(size == 0)
            return null;
        while (inRefresh) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        return values.get(random.nextInt(size));
    }

    @Override
    public String getExpress() {
        return "{"+name+"}";
    }

    public int getRefresh() {
        return refresh;
    }

    public void setRefresh(int refresh) {
        this.refresh = refresh;
    }
}
