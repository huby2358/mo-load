package io.mo.tsbs;
import com.github.javafaker.Faker;
import io.mo.tsbs.cpuonly.InsertSQLStmt;
import io.mo.util.TSBSConfUtil;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class SQLStmtBuffer {
    private int read_pos = 0;//当前读的位置
    private int write_pos = 0;//当前写的位置
    
    private long read_time = 0;//当前读的总次数
    private long write_time = 0;//当前写的总次数

    private Random random = new Random();
    
    private int bufferSize = TSBSConfUtil.getBufferSize();
    
    private SQLStmtInf template = null;
    private SQLStmtInf stmts[] = null;

    private DateFormat dt_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private Faker faker = new Faker(Locale.US);
    private static Logger LOG = Logger.getLogger(SQLStmtBuffer.class.getName());
    
    
    public SQLStmtBuffer(SQLStmtInf template){
        this.template = template;
        stmts = new SQLStmtInf[bufferSize];
    }

    public void addSQLStmt(SQLStmtInf sql){

        if(write_time - read_time > stmts.length -1){
            return;
        }

        if(write_pos == stmts.length){
            write_pos = 0;
            write_time++;
        }
        stmts[write_pos] = sql;
        write_pos ++;
        write_time++;
    }

    public void add(){
        if(write_time - read_time > stmts.length -1){
            //LOG.debug("The trans buffer is full, do not need to supply.");
            return;
        }
        
        addSQLStmt(template.newInstance());
    }


    public SQLStmtInf getSQLStmt(){
        SQLStmtInf stmt = stmts[read_pos];
        if(read_pos == stmts.length -1){
            read_pos = 0;
            read_time++;
        }
        else{
            read_pos++;
            read_time++;
        }
        
        return stmt;
    }
    public void fill(){
        write_pos = 0;
        for(int i = 0; i < stmts.length;i++){
            stmts[i] = template.newInstance();
            write_pos++;
            write_time++;
        }
    }
    
    public static void main(String[] args){
        SQLStmtBuffer buffer = new SQLStmtBuffer(new InsertSQLStmt());
        //System.out.println(buffer.newInsertStmt());
        //LOG.info(buffer.newLoadInlineStmt());
        long start = System.currentTimeMillis();
        buffer.fill();
        long end = System.currentTimeMillis();
        System.out.println("end - start = "+(end - start));
    }
}
