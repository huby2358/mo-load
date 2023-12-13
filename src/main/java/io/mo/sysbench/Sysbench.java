package io.mo.sysbench;

import io.mo.CONFIG;
import io.mo.conn.ConnectionOperation;
import io.mo.util.SysbenchConfUtil;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sysbench {
    public static String db_name = SysbenchConfUtil.getSysbenchDb();
    public static String tbl_prefix = SysbenchConfUtil.getSysbenchTablePrefix();
    public static int tbl_conut = SysbenchConfUtil.getSysbenchTableCount();
    public static int tbl_size = SysbenchConfUtil.getSysbenchTableSize();
    
    public static int batch_size = SysbenchConfUtil.getSysbenchBatchSize();
    
    public static String auto_incr = SysbenchConfUtil.getSysbenchAutoIncrement(); 
    
    public static int loadWorkerNum = SysbenchConfUtil.getLoaderWoker();
    
    public static Random random = new Random();

    private static Logger LOG = Logger.getLogger(Sysbench.class.getName());
    
    public Sysbench(){
        
    }
    
    public static void main(String[] args){

        Options options = new Options();
        options.addOption("h",true,"The server or proxy address");
        options.addOption("P",true,"The server or proxy port");
        options.addOption("u",true,"The username of conneciton to server");
        options.addOption("p",true,"The password of connection user to server");
        options.addOption("t",true,"The thread number per transaction");
        options.addOption("d",true,"The duration that all transactions will run");
        options.addOption("b","db",true,"The duration that all transactions will run");
        options.addOption("n",true,"For sysbench data prepare, set table count, must designate method to SYSBENCH ");
        options.addOption("s",true,"for sysbench data prepare, set table size, must designate method to SYSBENCH");

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

            if(cmd.hasOption("n"))
                tbl_conut = Integer.parseInt(cmd.getOptionValue('n'));

            if(cmd.hasOption("s"))
                tbl_size = Integer.parseInt(cmd.getOptionValue('s'));

            if(cmd.hasOption("t"))
                loadWorkerNum = Integer.parseInt(cmd.getOptionValue('t'));

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        
        String db_drop_ddl = "DROP DATABASE IF EXISTS `" + db_name +"`";

        String db_create_ddl = "CREATE DATABASE IF NOT EXISTS `" + db_name +"`";
        
        String insert_dml = "INSERT INTO `tablename` VALUES(?,?,?,?)";
        String insert_auto_dml = "INSERT INTO `tablename`(`k`,`c`,`pad`) VALUES(?,?,?)";
        String synccommit = "select mo_ctl('cn','synccommit','')";

        ExecutorService executor = Executors.newFixedThreadPool(loadWorkerNum);
        CountDownLatch latch = new CountDownLatch(tbl_conut);
        
        try {
            Connection con = ConnectionOperation.getConnection();
            Statement stmt = con.createStatement();
            
            //create database
            LOG.info(String.format("Now start to initialize sysbench data, db=%s, tableCount=%d, tableSize=%d",db_name,tbl_conut,tbl_size));
            stmt.execute(db_drop_ddl);
            LOG.info(String.format("Succeeded to drop database[%s]",db_name));
            stmt.execute(synccommit);
            LOG.info(String.format("Succeeded to sync commit",db_name));
            stmt.execute(db_create_ddl);
            LOG.info(String.format("Succeeded to create database[%s]",db_name));
            stmt.execute(synccommit);
            LOG.info(String.format("Succeeded to sync commit",db_name));
            stmt.close();
            con.close();
            
            for(int i = 1; i < tbl_conut + 1 ; i++) {
                Connection conLoad = ConnectionOperation.getConnection();
                conLoad.setCatalog(db_name);
                if (conLoad == null) {
                    LOG.error(" mo-load can not get invalid connection after trying 3 times, and the program will exit");
                    System.exit(1);
                }
                
                SysBenchLoader loader = new SysBenchLoader(conLoad, i, tbl_size, auto_incr.equalsIgnoreCase("true"), batch_size, latch);
                executor.execute(loader);
            }
            latch.await();
            executor.shutdown();
            LOG.info(String.format("Finished to initialize sysbench data, db=%s, tableCount=%d, tableSize=%d",db_name,tbl_conut,tbl_size));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static int getRandom4Number(){
        return random.nextInt(9000)+1000;
    }
    
    public static String getRandomChar(int len){
        String[] chars = new String[] { "0","1", "2", "3", "4", "5", "6", "7", "8", "9" };
        int count = len/11;
        Random r = new Random();
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");;
        for(int j = 0; j < count; j++) {
            for (int i = 0; i < 11; i++) {
                int index = r.nextInt(10);
                shortBuffer.append(chars[index]);
            }
            if( j != count -1)
                shortBuffer.append("-");
        }
        return shortBuffer.toString();
    }
    
    
    
}
