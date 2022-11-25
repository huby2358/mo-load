package io.mo.thread;

import io.mo.Sysbench;
import io.mo.util.SysbenchConfUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static io.mo.Sysbench.tbl_size;

public class SysBenchLoader implements Runnable{
    
    private int id;
    private int size;
    
    private boolean auto_incr = false;
    private Random random = new Random();
    
    private CountDownLatch latch = null;
    
    private Connection con = null;
    
    private String tbl_prefix = SysbenchConfUtil.getSysbenchTablePrefix();

    String tbl_create_ddl = "CREATE TABLE IF NOT EXISTS`tablename` (\n" +
            "`id` INT NOT NULL,\n" +
            "`k` INT DEFAULT 0,\n" +
            "`c` CHAR(120) DEFAULT NULL,\n" +
            "`pad` CHAR(60) DEFAULT NULL ,\n" +
            "PRIMARY KEY (`id`)\n" +
            ")";
    String tbl_create_auto_ddl = " CREATE TABLE IF NOT EXISTS `tablename` (\n" +
            "`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "`k` INT DEFAULT 0,\n" +
            "`c` CHAR(120) DEFAULT NULL ,\n" +
            "`pad` CHAR(60) DEFAULT NULL ,\n" +
            "PRIMARY KEY (`id`)\n" +
            ")";
    
    private String insert_dml = "INSERT INTO `%s` VALUES(%d,%d,'%s','%s')";
    private String insert_auto_dml = "INSERT INTO `%s`(`k`,`c`,`pad`) VALUES(%d,'%s','%s')";


    private static Logger LOG = Logger.getLogger(SysBenchLoader.class.getName());
    public SysBenchLoader(Connection con, int id, int size, boolean auto_incr, CountDownLatch latch){
        this.id = id;
        this.size = size;
        this.con = con;
        this.latch = latch;
        this.auto_incr = auto_incr;
    }
    
    @Override
    public void run() {
        String tbl_name = tbl_prefix + id;
        LOG.info(String.format("Initialize table %s and load %d data.....", tbl_name, size));
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            
            if (auto_incr) {
                //create table
                stmt.execute(tbl_create_auto_ddl.replace("tablename", tbl_name));
                //batch insert
                long start = System.currentTimeMillis();
                for (int j = 1; j < tbl_size + 1; j++) {
                    String sql = String.format(insert_auto_dml,tbl_name,getRandom4Number(),getRandomChar(120),getRandomChar(60));
                    stmt.execute(sql);
                }
                long end = System.currentTimeMillis();
                LOG.info(String.format("Table %s has been initialized completely, and cost:%s s", tbl_name, (end - start) / 1000));
            } else {
                //create table
                stmt.execute(tbl_create_ddl.replace("tablename", tbl_name));
                //batch insert
                long start = System.currentTimeMillis();
                for (int j = 1; j < tbl_size + 1; j++) {
                    String sql = String.format(insert_dml,tbl_name,j,getRandom4Number(),getRandomChar(120),getRandomChar(60));
                    stmt.execute(sql);
                }
                long end = System.currentTimeMillis();
                LOG.info(String.format("Table %s has been initialized completely, and cost:%s s", tbl_name, (end - start) / 1000));
            }
            stmt.close();
            con.close();
        } catch (SQLException e) {
            LOG.error("Unexpected exception: "+e.getMessage());
            LOG.error("The program wil exit, please check the reason.");
            System.exit(1);
        }finally {
            latch.countDown();
        }
    }

    public int getRandom4Number(){
        return random.nextInt(9000)+1000;
    }

    public  String getRandomChar(int len){
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
