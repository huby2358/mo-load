package io.mo;

import io.mo.conn.ConnectionOperation;
import io.mo.thread.TestExecutor;
import io.mo.util.RunConfigUtil;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.sql.Connection;

public class DTest {
    private static Logger LOG = Logger.getLogger(DTest.class.getName());
    public static void main(String[] args){
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
                excuteTime = Integer.parseInt(cmd.getOptionValue('d'))*60*1000;

            if(cmd.hasOption("t"))
                t_num = Integer.parseInt(cmd.getOptionValue('t'));

            if(cmd.hasOption("shutdown"))
                CONFIG.SHUTDOWN_SYSTEMOUT = true;

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        LOG.info(String.format("The test will last for %d minutes.",excuteTime/1000/60));
        
        Thread[] threads = new Thread[t_num];
        for(int i = 0; i < t_num; i++){
            Connection connection  = ConnectionOperation.getConnection();
            TestExecutor executor = new TestExecutor(connection,i);
            threads[i] = new Thread(executor);
        }
        
        for(int i = 0; i < t_num; i++){
            threads[i].start();
        }

        long runT = 0;
        long interval = 5*1000;
        while(!CONFIG.TIMEOUT){
            if(runT >= excuteTime){
                CONFIG.TIMEOUT = true;
            }else{
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                runT += interval;
            }
        }
    }
}
