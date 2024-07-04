package io.mo;

import io.mo.conn.ConnectionOperation;
import io.mo.para.PreparedPara;
import io.mo.replace.Variable;
import io.mo.result.ExecResult;
import io.mo.thread.*;
import io.mo.transaction.PreparedSQLCommand;
import io.mo.transaction.SQLScript;
import io.mo.transaction.TransBuffer;
import io.mo.transaction.Transaction;
import io.mo.util.ReplaceConfigUtil;
import io.mo.util.RunConfigUtil;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class MOPerfTest {
    private static Transaction[] transactions;
    private static ExecResult[] execResult;
    private static ResultProcessor resultProcessor = new ResultProcessor();
    private static TransBufferProducer transBufferProducer = new TransBufferProducer();
    private static PreparedParaProducer preparedParaProducer = new PreparedParaProducer();

    //private static ExecutorService[] services;
    private static boolean exit_normally = false;
    
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static Logger LOG = Logger.getLogger(MOPerfTest.class.getName());
    
    
    public static void main(String[] args) throws InterruptedException {
        ShutDownHookThread hookThread = new ShutDownHookThread();
        Runtime.getRuntime().addShutdownHook(hookThread);

        //初始化结果目录
        initDir();

        //初始化变量
        initVar();

        long excuteTime = RunConfigUtil.getExecDuration()*60*1000;
        int t_num = 0;

        Options options = new Options();
        options.addOption("h",true,"The server or proxy address");
        options.addOption("P",true,"The server or proxy port");
        options.addOption("u",true,"The username of conneciton to server");
        options.addOption("p",true,"The password of connection user to server");
        options.addOption("t",true,"The thread number per transaction");
        options.addOption("d",true,"The duration that all transactions will run");
        options.addOption("b","db",true,"The duration that all transactions will run");
        options.addOption("g","shutdown",false,"shut down tracing real progress data by system-out");
        options.addOption("i","report-interval",true,"shut down tracing real progress data by system-out");
        
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options,args);
            
            if(cmd.hasOption("h")) {
                CONFIG.SPEC_SERVER_ADDR = cmd.getOptionValue('h');
                LOG.info("server addr = " + cmd.getOptionValue('h'));
            }
            
            if(cmd.hasOption("P")) {
                CONFIG.SPEC_SERVER_PORT = Integer.parseInt(cmd.getOptionValue('P'));
                LOG.info("server port = " + cmd.getOptionValue('P'));
            }
            
            if(cmd.hasOption("u")) {
                CONFIG.SPEC_USERNAME = cmd.getOptionValue('u');
                LOG.info("username = " + cmd.getOptionValue('u'));
            }
            
            if(cmd.hasOption("p")) {
                CONFIG.SPEC_PASSWORD = cmd.getOptionValue('p');
                LOG.info("password = " + cmd.getOptionValue('p'));
            }
            
            if(cmd.hasOption("b")) {
                CONFIG.SPEC_DATABASE = cmd.getOptionValue("b");
                LOG.info("database = " + cmd.getOptionValue("b"));
            }
            
            if(cmd.hasOption("d"))
                excuteTime = Long.parseLong(cmd.getOptionValue('d'))*60*1000;

            if(cmd.hasOption("t"))
                t_num = Integer.parseInt(cmd.getOptionValue('t'));

            if(cmd.hasOption("shutdown"))
                CONFIG.SHUTDOWN_SYSTEMOUT = true;

            if(cmd.hasOption("report-interval"))
                CONFIG.REPORT_INTERVAL = Integer.parseInt(cmd.getOptionValue("report-interval"))*1000;
            
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        LOG.info(String.format("The test will last for %d minutes.",excuteTime/1000/60));
        

        
        //初始化
        initTransaction();

        LOG.info("Initializing the execution threads,please wait for serval minutes.");
        for(int i = 0; i < transactions.length; i++){
            if(t_num == 0)
                t_num = transactions[i].getTheadnum();
            else {
                transactions[i].setTheadnum(t_num);
                execResult[i].setVuser(t_num);
            }
            
            LOG.info(String.format("transaction[%s].tnum = %d", transactions[i].getName(),t_num));
            TransExecutor[] executors = new TransExecutor[t_num];

            //定义线程初始化计数器
            CyclicBarrier barrier = new CyclicBarrier(t_num, new Runnable() {
                @Override
                public void run() {
                    LOG.info("All the he execution threads has been prepared and started running, pleas wait.....");
                    //实时计算性能测试结果数据
                    resultProcessor.start();
                }
            });

            long[] timeCost = new long[5];
            Thread[] thread = new Thread[t_num];
            for(int j = 0;j < t_num;j++){
                try {
                    long t0 = System.currentTimeMillis();

                    //获取db连接，每个executor负责一个链接
                    LOG.debug(String.format("Connection[%d] startTime: %s",j,format.format(new Date())));
                    Connection connection = ConnectionOperation.getConnection();
                    LOG.debug(String.format("Connection[%d] endTime: %s",j,format.format(new Date())));
                    if(connection == null){
                        LOG.error(" mo-load can not get invalid connection after trying 3 times, and the program will exit");
                        System.exit(1);
                    }
                    hookThread.addConnection(connection);

                    long t1 = System.currentTimeMillis();
                    timeCost[0] += t1 - t0;

                    //初始化发送缓冲区，每个executor拥有一个发送缓冲区
                    TransBuffer buffer = new TransBuffer(transactions[i]);
                    executors[j] = new TransExecutor(j,connection,buffer,execResult[i],barrier);

                    long t2 = System.currentTimeMillis();
                    timeCost[1] += t2 - t1;

                    if(!transactions[i].isPrepared()){
                        //将该加入到发送缓冲区生成器队列中，用于重新补充缓冲区中已经被发送过的事务
                        transBufferProducer.addBuffer(buffer);
                        //执行前，先初始化并填满每个线程的发送队列
                        buffer.fill();

                        timeCost[2] += System.currentTimeMillis() - t2;
                    }else {
                        PreparedSQLCommand[] commands = transactions[i].getScript().getPreparedCommands();
                        for(int k = 0; k < commands.length; k++){
                            PreparedPara[] preparedParas = commands[k].newPreparedParas();
                            for(int h = 0; h < preparedParas.length; h++){
                                preparedParaProducer.addPreparedPara(preparedParas[h]);
                            }

                            PrepareStatmentExecutor prepareStatmentExecutor = new PrepareStatmentExecutor(connection,commands[k],preparedParas);
                            if(!prepareStatmentExecutor.prepare()){
                                System.exit(1);
                            }

                            executors[j].addPrepareStatmentExecutor(prepareStatmentExecutor);
                        }
                        timeCost[3] += System.currentTimeMillis() - t2;
                    }

                    long t3 = System.currentTimeMillis();
                    thread[j] = new Thread(executors[j]);
                    timeCost[4] += System.currentTimeMillis() - t3;
                    //services[i].execute(executors[j]);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }

            for (int j = 0; j < 5; ++j) {
                LOG.info(String.format("part %d cost: %d (s)", j, timeCost[j] / 1000));
            }
            LOG.info("All the he execution threads has been prepared,and start running.......");

            //启动所有执行线程
            for(int j = 0; j < thread.length; j++){
                thread[j].start();
            }
        }

        if(transBufferProducer.getBuffers().size() != 0){
            //启动发送缓冲区生成器(线程)，循环生产新的事务脚本到缓冲区中
            transBufferProducer.start();
        }
        
        if(preparedParaProducer.getBuffers().size() != 0){
            //启动发送缓冲区生成器(线程)，循环生产新的事务脚本到缓冲区中
            preparedParaProducer.start();
        }


        //实时计算性能测试结果数据
        resultProcessor.start();
        
        //等待所有线程执行
        long runT = 0;
        long interval = 5*1000;
        while(!CONFIG.TIMEOUT){
            if(runT >= excuteTime){
                CONFIG.TIMEOUT = true;
            }else{
                Thread.sleep(interval);
                runT += interval;
            }
        }

        transBufferProducer.setTerminated(true);

        Thread.sleep(3000);

        LOG.info("write total time = "+transBufferProducer.getWrite_total()+",read total time = "+transBufferProducer.getRead_total());
        resultProcessor.join();
        
        if(resultProcessor.getTestResult()) {
            LOG.info("This test has been executed successfully, and can get detailed result data in ./report/");
        }
        else {
            LOG.error("This test has been executed failed, please check detailed result data in ./report/");
            System.exit(1);
        }

        exit_normally = true;
        /*for (ExecutorService service:services) {
            service.shutdown();
        }*/
    }

    //初始化当前执行的结果文件目录
    public static void initDir(){
        File error_dir = new File("report/" + CONFIG.EXECUTENAME + "/error/");

        if(!error_dir.exists())
            error_dir.mkdirs();
    }

    //初始化变量
    public static void initVar(){
        LOG.info("Initializing the variables,please wait for serval minutes.");
        if(0 == ReplaceConfigUtil.vars.size()){
            LOG.info("No variable item,skip initializing the variables");
            return;
        }

        for(int i = 0; i < ReplaceConfigUtil.vars.size(); i++){
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            var.init();
        }
        LOG.info("The variables has been prepared!");
    }

    //初始化事务先关实例
    public static void initTransaction() {
        int transCount = RunConfigUtil.getTransactionNum();
        if (0 == transCount) {
            LOG.error("No transaction needs to be executed,the program will exit.");
            System.exit(1);
        }

        transactions = new Transaction[transCount];
        execResult = new ExecResult[transCount];

        //定义线程池
        //services = new ExecutorService[transCount];

        for (int i = 0; i < transCount; i++) {
            transactions[i] = RunConfigUtil.getTransaction(i);
            execResult[i] = new ExecResult(transactions[i].getName(),transactions[i].getScript().length(),transactions[i].getTheadnum());
            resultProcessor.addResult(execResult[i]);
        }
    }

    static class ShutDownHookThread extends Thread{
        private List<Connection> conns = new ArrayList<Connection>();

        public void addConnection(Connection connection){
            conns.add(connection);
        }
        @Override
        public void run() {
            CONFIG.TIMEOUT = true;
            LOG.info("Program is shutting down,now will release the resources...");
            try {
                Thread.sleep(1000);
                for(int i = 0; i < conns.size();i++){
                    if(!(conns.get(i) == null || conns.get(i).isClosed() || !conns.get(i).isValid(1000))) {
                        if (!conns.get(i).getAutoCommit()) {
                            conns.get(i).rollback();
                        }
                        conns.get(i).close();
                    }
                }
                if(!exit_normally) {
                    LOG.info("write total time = "+transBufferProducer.getWrite_total()+",read total time = "+transBufferProducer.getRead_total());
                    resultProcessor.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                if(resultProcessor.getTestResult()) {
                    LOG.info("This test has been executed successfully, and can get detailed result data in ./report/");
                }
                else {
                    LOG.error("This test has been executed failed, please check detailed result data in ./report/");
                }
            }
        }
    }

}
