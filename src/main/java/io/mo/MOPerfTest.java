package io.mo;

import io.mo.conn.ConnectionOperation;
import io.mo.replace.Variable;
import io.mo.result.ExecResult;
import io.mo.thread.PreparedParaProducer;
import io.mo.thread.ResultProcessor;
import io.mo.thread.TransBufferProducer;
import io.mo.thread.TransExecutor;
import io.mo.transaction.TransBuffer;
import io.mo.transaction.Transaction;
import io.mo.util.ReplaceConfigUtil;
import io.mo.util.RunConfigUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class MOPerfTest {
    private static Transaction[] transactions;
    private static ExecResult[] execResult;
    private static ResultProcessor resultProcessor = new ResultProcessor();
    private static TransBufferProducer transBufferProducer = new TransBufferProducer();
    private static PreparedParaProducer preparedParaProducer = new PreparedParaProducer();

    //private static ExecutorService[] services;

    private static Logger LOG = Logger.getLogger(MOPerfTest.class.getName());

    //初始化当前执行的结果文件目录
    public static void initDir(){
        //File data_dirs = new File("report/data/");
        File error_dir = new File("report/error/");
//        if(!data_dirs.exists())
//            data_dirs.mkdirs();

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
            LOG.info("No transaction needs to be executed,the program will exit.");
        }

        transactions = new Transaction[transCount];
        execResult = new ExecResult[transCount];
        
        //定义线程池
        //services = new ExecutorService[transCount];

        for (int i = 0; i < transCount; i++) {
            transactions[i] = RunConfigUtil.getTransaction(i);
            transactions[i].setPreparedParaProducer(preparedParaProducer);
            execResult[i] = new ExecResult(transactions[i].getName());
            resultProcessor.addResult(execResult[i]);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ShutDownHookThread hookThread = new ShutDownHookThread();
        Runtime.getRuntime().addShutdownHook(hookThread);

        long excuteTime = RunConfigUtil.getExecDuration()*60*1000;
        int t_num = 0;

        if(args.length == 1){
            if(args[0] != null && !"0".equalsIgnoreCase(args[0])){
                excuteTime = Integer.parseInt(args[0])*60*1000;
            }
        }

        if(args.length == 2){
            if(args[0] != null && !"0".equalsIgnoreCase(args[0])){
                excuteTime = Integer.parseInt(args[0])*60*1000;
            }

            if(args[1] != null){
                t_num = Integer.parseInt(args[1]);
            }
        }
        
        LOG.info(String.format("The test will last for %d minutes.",excuteTime/1000/60));
        
        //初始化结果目录
        initDir();

        //初始化变量
        initVar();

        //初始化
        initTransaction();

        LOG.info("Initializing the execution threads,please wait for serval minutes.");
        for(int i = 0; i < transactions.length; i++){
            if(t_num == 0)
                t_num = transactions[i].getTheadnum();
            else
                transactions[i].setTheadnum(t_num);
            
            LOG.info(String.format("transaction[%s].tnum = %d", transactions[i].getName(),t_num));
            TransExecutor[] executors = new TransExecutor[t_num];

            //初始化线程组
            //services[i] = Executors.newFixedThreadPool(t_num);

            //定义线程初始化计数器
            CyclicBarrier barrier = new CyclicBarrier(t_num, new Runnable() {
                @Override
                public void run() {
                    LOG.info("All the he execution threads has been prepared and started running, pleas wait.....");
                    //实时计算性能测试结果数据
                    resultProcessor.start();
                }
            });

            Thread[] thread = new Thread[t_num];
            for(int j = 0;j < t_num;j++){
                try {
                    //获取db连接，每个executor负责一个链接
                    Connection connection = ConnectionOperation.getConnection();
                    if(connection == null){
                        LOG.error(" mo-load can not get invalid connection after trying 3 times, and the program will exit");
                        System.exit(1);
                    }
                    hookThread.addConnection(connection);

                    //初始化发送缓冲区，每个executor拥有一个发送缓冲区
                    TransBuffer buffer = new TransBuffer(transactions[i]);

                    if(!transactions[i].isPrepared()){
                        //将该加入到发送缓冲区生成器队列中，用于重新补充缓冲区中已经被发送过的事务
                        transBufferProducer.addBuffer(buffer);
                        //执行前，先初始化并填满每个线程的发送队列
                        buffer.fill();
                    }

                    executors[j] = new TransExecutor(j,connection,buffer,execResult[i],barrier);
                    thread[j] = new Thread(executors[j]);
                    //services[i].execute(executors[j]);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
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

        LOG.info(" write total time = "+transBufferProducer.getWrite_total()+",read total time = "+transBufferProducer.getRead_total());

        /*for (ExecutorService service:services) {
            service.shutdown();
        }*/
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
                    if(!conns.get(i).getAutoCommit()){
                        conns.get(i).rollback();
                    }
                    conns.get(i).close();
                }
                LOG.info("Program exit completely.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                //e.printStackTrace();
                LOG.info("Program exit completely.");
            }
        }
    }

}
