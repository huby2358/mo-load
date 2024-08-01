package io.mo.tsbs.cpuonly;

import com.github.javafaker.Faker;
import io.mo.tsbs.DATA;
import io.mo.tsbs.SQLStmtBuffer;
import io.mo.tsbs.SQLStmtInf;
import io.mo.util.TSBSConfUtil;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class InsertSQLStmt implements SQLStmtInf{
    private String sql = null;
    private int size;

    private StringBuilder stmtRecord = new StringBuilder(1200000);

    private boolean isTimestampDefault = TSBSConfUtil.IsTimestampDefault();

    private DateFormat dt_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private Faker faker = new Faker(Locale.US);

    private Random random = new Random();

    private static Logger LOG = Logger.getLogger(InsertSQLStmt.class.getName());
    
    public InsertSQLStmt(){
        size = TSBSConfUtil.getBufferSize();
    }

    public InsertSQLStmt(int size){
        this.size = size;
    }
    
    public InsertSQLStmt(String sql, int size){
        this.sql = sql;
        this.size = size;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }


    @Override
    public int metricCount() {
        return size*10;
    }

    @Override
    public int recordCount() {
        return size;
    }

    @Override
    public SQLStmtInf newInstance() {
        stmtRecord.setLength(0);
        if(!isTimestampDefault)
            stmtRecord.append("INSERT INTO `cpu` values");
        else
            stmtRecord.append("INSERT INTO `cpu`(tags,id,usage_user,usage_system,usage_idle,usage_nice,usage_iowait,usage_irq,usage_softirq,usage_steal,usage_guest,usage_guest_nice) values");
        for(int i = 0; i < size; i++) {
            stmtRecord.append("(");
            stmtRecord.append("'cpu',");
            stmtRecord.append("'");
            stmtRecord.append(getUniqueNumber());
            stmtRecord.append("',");
            if(!isTimestampDefault){
                stmtRecord.append("'");
                stmtRecord.append(nextDateTime());
                stmtRecord.append("'");
                stmtRecord.append(",");
            }
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(",");
            stmtRecord.append(nextFloat());
            stmtRecord.append(")");
            if(i == size -1 )
                stmtRecord.append(";");
            else
                stmtRecord.append(",");
        }
        //LOG.info("size = " + stmtRecord.length());
        return new LoadSQLStmt(stmtRecord.toString(),size);
    }

    @Override
    public String getSQL() {
        return sql;
    }

    public long getUniqueNumber(){
        return System.nanoTime();
    }

    //fake random decimal(l,s)
    public float nextFloat(){
        return random.nextFloat()*100;
    }

    public String nextDateTime(){
        return dt_format.format(faker.date().between(DATA.START_DATE,DATA.END_DATE));
    }
    
    public static void main(String[] args){
        SQLStmtInf stmt = new InsertSQLStmt(10);
        System.out.println(stmt.newInstance().getSQL());
    }

}
