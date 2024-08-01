package io.mo.tsbs.cpuonly;

import io.mo.conn.ConnectionOperation;
import io.mo.tsbs.*;
import io.mo.util.TSBSConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;

public class Loader implements Runnable{
    
    private int id;
    
    private int batch_size;
    
    private String dbName = TSBSConfUtil.getTsdsDb();
    
    private SQLStmtBuffer buffer = null;
    
    private CountDownLatch latch = null;
    
    private Statement statement = null;
    
    private Connection con = null;
    
    public static boolean TIMEOUT = false;
    
    private int method = TSBSConfUtil.getMethod();

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    
    private static Logger LOG = Logger.getLogger(Loader.class.getName());
    
    public Loader(Connection con, int id, int batch_size, CountDownLatch latch){
        this.id = id;
        this.batch_size = batch_size;
        this.con = con;
        this.latch = latch;
        
        if(method == 0)
            buffer = new SQLStmtBuffer(new InsertSQLStmt());
        
        if(method == 1)
            buffer = new SQLStmtBuffer(new LoadSQLStmt());
        
    }
    
    @Override
    public void run() {
        LOG.info(String.format("Loadworker[%d] has been started, and begin to prepare data....", id));
        
        //prepare buffer data
        buffer.fill();
        
        try {
            latch.wait();

            statement = con.createStatement();
            String sql = null;
            
            while(!TIMEOUT){
                SQLStmtInf sqlStmt = buffer.getSQLStmt();
                long startTime = System.currentTimeMillis();
                long endTime;
                try{
                    startTime = System.currentTimeMillis();
                    statement.execute(sqlStmt.getSQL());
                    endTime = System.currentTimeMillis();

                    ResultRecord record = new ResultRecord(startTime,endTime,sqlStmt.metricCount(),sqlStmt.recordCount());
                    ResultCalculator.addRecord(record);
                }catch (SQLException e) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setRequestTime(simpleDateFormat.format(startTime));
                    errorMessage.setSql(sqlStmt.getSQL());
                    errorMessage.setSqlFileName(System.currentTimeMillis()+".sql");
                    errorMessage.setErrorCode(e.getErrorCode());
                    errorMessage.setErrorMessage(e.getMessage());

                    try {
                        if(con == null || con.isClosed() || !con.isValid(1000)) {
                            if(!con.isClosed()){
                                con.close();
                            }
                            con = ConnectionOperation.getConnection();
                            if(con == null){
                                //running = false;
                                LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
                                break;
                            }
                            statement = con.createStatement();
                            continue;
                        }
                        //statement.execute("rollback;");
                        //connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    } catch (Exception ex) {
                        e.printStackTrace();
                    }
                }
            }
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
    
    public static void main(String[] args){
        //TSDSLoader loader = new TSDSLoader(null,0,0,true,0,null);
       // System.out.println(loader.getRandomChar(60));
        
    }
}
