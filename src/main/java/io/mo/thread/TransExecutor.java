package io.mo.thread;

import io.mo.CONFIG;
import io.mo.conn.ConnectionOperation;
import io.mo.para.PreparedPara;
import io.mo.result.ExecResult;
import io.mo.result.RTBuffer;
import io.mo.transaction.PreparedSQLCommand;
import io.mo.transaction.SQLScript;
import io.mo.transaction.TransBuffer;
import io.mo.transaction.Transaction;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CyclicBarrier;

public class TransExecutor implements Runnable {
    private static Logger LOG = Logger.getLogger(TransExecutor.class.getName());

    private TransBuffer transBuffer;

    private RTBuffer rtBuffer;

    private ExecResult execResult;

    private Connection connection;

    private Statement statement;
    
    private PrepareStatmentExecutor[] prepareStatmentExecutors;

    private boolean running = true;

    private String transName;

    private Transaction transaction;

    private CyclicBarrier barrier;
    
    private int id;
    

    public TransExecutor(int id , Connection connection,TransBuffer transBuffer,ExecResult execResult,CyclicBarrier barrier){
        this.transBuffer = transBuffer;
        this.transName = transBuffer.getTransaction().getName();

        this.execResult = execResult;
        this.execResult.increaseThread();

        this.rtBuffer = new RTBuffer(execResult);
        this.transaction = this.transBuffer.getTransaction();

        this.connection = connection;
        this.barrier = barrier;
        this.id = id;
        try {
            int count = transaction.getScript().length();
            if(transaction.isPrepared()){
                prepareStatmentExecutors = new PrepareStatmentExecutor[count];
            }else {
                statement = this.connection.createStatement();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPrepareStatmentExecutor(PrepareStatmentExecutor executor){
        for(int i = 0; i < prepareStatmentExecutors.length; i++){
            if(prepareStatmentExecutors[i] == null){
                prepareStatmentExecutors[i] = executor;
                break;
            }
        }
    }

    @Override
    public void run() {
        String sql = null;
        //如果是事务模式，则启动事务执行
        if(transaction.getMode() == CONFIG.DB_TRANSACTION_MODE){

                //启动事务
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(0);
            }
            
            if(!transaction.isPrepared()){
                while(!CONFIG.TIMEOUT) {
                    SQLScript script = transBuffer.getScript();

                    try {
                        long beginTime = System.currentTimeMillis();
                        //statement.execute("begin;");
                        for (int i = 0; i < script.length(); i++) {
                            boolean rs = statement.execute(script.getCommand(i));
                        }
                        //statement.execute("commit;");
                        connection.commit();
                        long endTime = System.currentTimeMillis();
                        //将执行时间和结果保存在临时缓冲区里
                        rtBuffer.setValue(transName + "=" + beginTime + ":" + endTime);
                    } catch (SQLException e) {
                        //LOG.error(e.getStackTrace());
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times in 30s, and will exit",id));
                                    break;
                                }
                                statement = connection.createStatement();
                                connection.setAutoCommit(false);
                                execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                                continue;
                            }
                            //statement.execute("rollback;");
                            connection.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                        continue;
                    }
                }
            }
            
            else{
                SQLScript script = transaction.getScript();
                while(!CONFIG.TIMEOUT){
                    try {
                        long beginTime = System.currentTimeMillis();
                        for (int i = 0; i < script.length(); i++) {
                            prepareStatmentExecutors[i].execute();
                        }
                        
                        connection.commit();
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        rtBuffer.setValue(transName+"="+beginTime+":"+endTime);
                    }catch (SQLException e){
                        
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                        
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times in 30s, and will exit",id));
                                    break;
                                }
                                connection.setAutoCommit(false);
                                for(int i = 0; i < script.length(); i++){
                                    prepareStatmentExecutors[i].setConnection(connection);
                                    if(!prepareStatmentExecutors[i].prepare()){
                                        System.exit(1);
                                    }
                                }
                                continue;
                            }
                            connection.rollback();
                            
                        } catch (SQLException e1) {
                            running = false;
                            rtBuffer.setValid(false);
                            LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times in 30s, and will exit",id));
                            break;
                        } 
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                    }
                }
            }
        }
        
        else {
            //如果是非事务模式，直接执行
            //如果没有prepare
            if(!transaction.isPrepared()){

                while(!CONFIG.TIMEOUT){
                    SQLScript script = transBuffer.getScript();
                    try {
                        long beginTime = System.currentTimeMillis();
                        for(int i = 0; i < script.length();i++){
                            boolean rs = statement.execute(script.getCommand(i));
                        }
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        rtBuffer.setValue(transName+"="+beginTime+":"+endTime);

                    } catch (SQLException e) {
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
                                    break;
                                }
                                statement = connection.createStatement();
                                execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                                continue;
                            }
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                    }
                }
            }
            
            else {

                SQLScript script = transaction.getScript();
                while(!CONFIG.TIMEOUT){
                    try {
                        long beginTime = System.currentTimeMillis();
                        for (int i = 0; i < script.length(); i++) {
                            prepareStatmentExecutors[i].execute();
                        }
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        rtBuffer.setValue(transName+"="+beginTime+":"+endTime);
                    }catch (SQLException e){
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
                                    break;
                                }
                                for(int i = 0; i < script.length(); i++){
                                    prepareStatmentExecutors[i].setConnection(connection);
                                    if(!prepareStatmentExecutors[i].prepare()){
                                        System.exit(1);
                                    }
                                }
                                continue;
                            }
                            
                        } catch (SQLException e1) {
                            running = false;
                            rtBuffer.setValid(false);
                            LOG.error(String.format("Thread[id=%d] will exit for unexpected exception: \n %s",id,e.getMessage()));
                            break;
                        }
                        execResult.setError(transName+":\r\n"+e.getMessage()+"\r\n");
                    }
                }
            }
        }

        running = false;
        rtBuffer.setValid(false);
    }
}
