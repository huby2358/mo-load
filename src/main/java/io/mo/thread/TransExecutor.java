package io.mo.thread;

import io.mo.CONFIG;
import io.mo.conn.ConnectionOperation;
import io.mo.para.PreparedPara;
import io.mo.result.ErrorMessage;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class TransExecutor implements Runnable {
    private static Logger LOG = Logger.getLogger(TransExecutor.class.getName());
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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
        
        this.execResult.setExpRate(this.transaction.getSucrate());
        
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
        String currentSql = null;
        PrepareStatmentExecutor currentPrepareExecutor = null;
        
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
                    long currentRequestTime = 0;
                    try {
                        long beginTime = System.currentTimeMillis();
                        //statement.execute("begin;");
                        for (int i = 0; i < script.length(); i++) {
                            currentSql = script.getCommand(i);
                            currentRequestTime = System.currentTimeMillis();
                            boolean rs = statement.execute(currentSql);
                        }
                        //statement.execute("commit;");
                        connection.commit();
                        long endTime = System.currentTimeMillis();
                        //将执行时间和结果保存在临时缓冲区里
                        //rtBuffer.setValue(transName + "=" + beginTime + ":" + endTime);
                        rtBuffer.setValue(beginTime,endTime);
                    } catch (SQLException e) {
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setRequestTime(simpleDateFormat.format(currentRequestTime));
                        errorMessage.setTxnName(transName);
                        errorMessage.setSql(currentSql);
                        errorMessage.setErrorCode(e.getErrorCode());
                        errorMessage.setErrorMessage(e.getMessage());
                        if(!this.transaction.isAcceptableError(e.getErrorCode())){
                            execResult.setError(errorMessage,false);
                            LOG.error(errorMessage.toString());
                        }else {
                            execResult.setError(errorMessage,true);
                            errorMessage.setExpected("true");
                            continue;
                        }
                        
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                if(!connection.isClosed()){
                                    connection.close();
                                }
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
                                    break;
                                }
                                statement = connection.createStatement();
                                connection.setAutoCommit(false);
                                continue;
                            }
                            //statement.execute("rollback;");
                            connection.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            else{
                SQLScript script = transaction.getScript();
                while(!CONFIG.TIMEOUT){
                    long currentRequestTime = 0;
                    try {
                        long beginTime = System.currentTimeMillis();
                        for (int i = 0; i < script.length(); i++) {
                            currentPrepareExecutor = prepareStatmentExecutors[i];
                            currentRequestTime = System.currentTimeMillis();
                            prepareStatmentExecutors[i].execute();
                        }
                        
                        connection.commit();
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        //rtBuffer.setValue(transName+"="+beginTime+":"+endTime);
                        rtBuffer.setValue(beginTime,endTime);
                    }catch (SQLException e){

                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setRequestTime(simpleDateFormat.format(currentRequestTime));
                        errorMessage.setTxnName(transName);
                        errorMessage.setSql(currentPrepareExecutor.getCurrentSql());
                        errorMessage.setParas(currentPrepareExecutor.getCurrentParas());
                        errorMessage.setErrorCode(e.getErrorCode());
                        errorMessage.setErrorMessage(e.getMessage());
                        if(!this.transaction.isAcceptableError(e.getErrorCode())){
                            execResult.setError(errorMessage,false);
                            LOG.error(errorMessage.toString());
                        }else {
                            errorMessage.setExpected("true");
                            execResult.setError(errorMessage,true);
                            continue;
                        }
                        
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                if(!connection.isClosed()){
                                    connection.close();
                                }
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
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
                    long currentRequestTime = 0;
                    try {
                        long beginTime = System.currentTimeMillis();
                        for(int i = 0; i < script.length();i++){
                            currentSql = script.getCommand(i);
                            currentRequestTime = System.currentTimeMillis();
                            boolean rs = statement.execute(currentSql);
                        }
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        //rtBuffer.setValue(transName+"="+beginTime+":"+endTime);
                        rtBuffer.setValue(beginTime,endTime);
                    } catch (SQLException e) {

                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setRequestTime(simpleDateFormat.format(currentRequestTime));
                        errorMessage.setTxnName(transName);
                        errorMessage.setSql(currentSql);
                        errorMessage.setErrorCode(e.getErrorCode());
                        errorMessage.setErrorMessage(e.getMessage());
                        if(!this.transaction.isAcceptableError(e.getErrorCode())){
                            execResult.setError(errorMessage,false);
                            LOG.error(errorMessage.toString());
                        }else {
                            execResult.setError(errorMessage,true);
                            errorMessage.setExpected("true");
                            continue;
                        }
                        
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                if(!connection.isClosed()){
                                    connection.close();
                                }
                                connection = ConnectionOperation.getConnection();
                                if(connection == null){
                                    running = false;
                                    rtBuffer.setValid(false);
                                    LOG.error(String.format("Thread[id=%d] can not get invalid connection after trying 3 times, and will exit",id));
                                    break;
                                }
                                statement = connection.createStatement();
                            }
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            else {

                SQLScript script = transaction.getScript();
                while(!CONFIG.TIMEOUT){
                    long currentRequestTime = 0;
                    try {
                        long beginTime = System.currentTimeMillis();
                        for (int i = 0; i < script.length(); i++) {
                            currentPrepareExecutor = prepareStatmentExecutors[i];
                            currentRequestTime = System.currentTimeMillis();
                            prepareStatmentExecutors[i].execute();
                        }
                        long endTime = System.currentTimeMillis();

                        //将执行时间和结果保存在临时缓冲区里
                        //rtBuffer.setValue(transName+"="+beginTime+":"+endTime);
                        rtBuffer.setValue(beginTime,endTime);
                    }catch (SQLException e){

                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setRequestTime(simpleDateFormat.format(currentRequestTime));
                        errorMessage.setTxnName(transName);
                        errorMessage.setSql(currentPrepareExecutor.getCurrentSql());
                        errorMessage.setParas(currentPrepareExecutor.getCurrentParas());
                        errorMessage.setErrorCode(e.getErrorCode());
                        errorMessage.setErrorMessage(e.getMessage());
                        if(!this.transaction.isAcceptableError(e.getErrorCode())){
                            execResult.setError(errorMessage,false);
                            LOG.error(errorMessage.toString());
                        }else {
                            execResult.setError(errorMessage,true);
                            errorMessage.setExpected("true");
                            continue;
                        }
                        
                        try {
                            if(connection == null || connection.isClosed() || !connection.isValid(1000)) {
                                if(!connection.isClosed()){
                                    connection.close();
                                }
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
                            }
                            
                        } catch (SQLException e1) {
                            running = false;
                            rtBuffer.setValid(false);
                            LOG.error(String.format("Thread[id=%d] will exit for unexpected exception: \n %s",id,e.getMessage()));
                            break;
                        }
                    }
                }
            }
        }

        running = false;
        rtBuffer.setValid(false);
        LOG.debug(String.format("Thread[id=%d] has been stoped.",id));
    }
    
    public static void main(String[] args){
        System.out.println(simpleDateFormat.format(System.currentTimeMillis()));
    }
}
