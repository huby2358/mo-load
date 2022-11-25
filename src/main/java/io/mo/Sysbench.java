package io.mo;

import io.mo.conn.ConnectionOperation;
import io.mo.thread.SysBenchLoader;
import io.mo.util.SysbenchConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
    
    public static String auto_incr = SysbenchConfUtil.getSysbenchAutoIncrement(); 
    
    public static Random random = new Random();

    private static Logger LOG = Logger.getLogger(Sysbench.class.getName());
    
    public static void main(String[] args){
        if(args.length == 1){
            if(args[0] != null){
                tbl_conut = Integer.parseInt(args[0]);
            }
        }

        if(args.length == 2){
            if(args[0] != null){
                tbl_conut = Integer.parseInt(args[0]);
            }

            if(args[1] != null){
                tbl_size = Integer.parseInt(args[1]);
            }
        }
        
        String db_drop_ddl = "DROP DATABASE IF EXISTS `" + db_name +"`";

        String db_create_ddl = "CREATE DATABASE IF NOT EXISTS `" + db_name +"`";
        
        

        String insert_dml = "INSERT INTO `tablename` VALUES(?,?,?,?)";
        String insert_auto_dml = "INSERT INTO `tablename`(`k`,`c`,`pad`) VALUES(?,?,?)";

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(tbl_conut);
        
        try {
            Connection con = ConnectionOperation.getConnection();
            Statement stmt = con.createStatement();
            
            //create database
            LOG.info(String.format("Now start to initialize sysbench data, db=%s, tableCount=%d, tableSize=%d",db_name,tbl_conut,tbl_size));
            stmt.execute(db_drop_ddl);
            stmt.execute(db_create_ddl);
            stmt.close();
            con.close();
            
            for(int i = 1; i < tbl_conut + 1 ; i++) {
                Connection conLoad = ConnectionOperation.getConnection();
                if (conLoad == null) {
                    LOG.error(" mo-load can not get invalid connection after trying 3 times, and the program will exit");
                    System.exit(1);
                }
                
                SysBenchLoader loader = new SysBenchLoader(conLoad, i, tbl_size, auto_incr.equalsIgnoreCase("true"), latch);
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
