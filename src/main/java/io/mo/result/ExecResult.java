package io.mo.result;

import io.mo.CONFIG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.math.Quantiles;
import org.checkerframework.checker.units.qual.A;

public class ExecResult {
    private String name;

    private long max_rt = -1;
    private long avg_rt = 0;
    private long min_rt = -1;
    
    private long report_total = 0;
    private long report_account = 0;
    private long report_start = 0;
    private long report_end = 0;
    private double rt_25th = -1;

    private double rt_75th = -1;
    private double rt_90th = -1;

    private double rt_99th = -1;
    
    private double expRate = 1;
    
    //private ArrayList<Long> rtValues = new ArrayList<>(10000000);
    
    private ReentrantLock lock = new ReentrantLock();


    private AtomicLong totalTime = new AtomicLong(0);
    private AtomicLong totalCount = new AtomicLong(0);
    private long errorCount = 0;

    private int tps = 0;

    private int qps = 0;

    private long start = 0;

    private long end = 0;

    

    public int queryCount = 1;

    

    public String startTime = null;
    public String endTime = null;
    public int vuser = 0;


    private int threadnum = 0;


    private List<ErrorMessage> errors = new ArrayList<>(CONFIG.TEMP_ERROR_BUF_SIZE);


    private  FileWriter error_writer ;


    private boolean terminated = false;
    
    private volatile double counter = 0;
    private volatile int index = 0;
    

    public int getThreadnum() {
        return threadnum;
    }

    public synchronized void increaseThread() {
        this.threadnum++;
    }

    public synchronized void decreaseThread() {
        this.threadnum--;
    }


    public ExecResult(String name){
        this.name = name;
        File res_dir = new File("report/"+CONFIG.EXECUTENAME+"/");
        if(!res_dir.exists())
            res_dir.mkdirs();
        try {
            error_writer = new FileWriter("report/"+ CONFIG.EXECUTENAME +"/error/" + name + ".err");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ExecResult(String name,int queryCount, int vuser){
        this.name = name;
        this.queryCount = queryCount;
        File res_dir = new File("report/"+CONFIG.EXECUTENAME+"/");
        if(!res_dir.exists())
            res_dir.mkdirs();
        try {
            error_writer = new FileWriter("report/" + CONFIG.EXECUTENAME +"/error/" + name + ".err");
        } catch (IOException e) {
            e.printStackTrace();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        this.startTime = dateFormat.format(now);
        
        this.vuser = vuser;
    }

    public synchronized void setTime(String time){
        String[] temp = time.split("=");
        String[] period = temp[1].split(":");
        long start = Long.parseLong(period[0]);
        long end = Long.parseLong(period[1]);
        setTime(start,end);
    }

    
    public synchronized void setTime(long start,long end){
        totalTime.addAndGet(end - start); 
        totalCount.incrementAndGet();
        long time = end - start;
        
        report_account++;
        report_total += time;
        

        if(max_rt < 0)  max_rt = time;
        if(min_rt < 0)  min_rt = time;

        if(this.start  == 0) this.start  = start;
        if(this.report_start  == 0) this.report_start  = start;
        
        if(this.report_end  == 0) this.report_end  = end;
        if(this.end == 0)  this.end = end;

        if(max_rt < time)  max_rt = time;

        if(min_rt > time)  min_rt = time;

        if(this.start > start)  this.start = start;

        if(this.report_start  > start) this.report_start  = start;

        if(this.end < end) this.end = end;
        if(this.report_end < end) this.report_end = end;
       
    }
    
     
    
//    public void computePercentile(){
//        Map<Integer,Double> percentile = Quantiles.percentiles().indexes(25,75,90,99).compute(rtValues);
//        rt_25th = percentile.get(25);
//        rt_75th = percentile.get(75);
//        rt_90th = percentile.get(90);
//        rt_99th = percentile.get(99);
//    }
//    
//    public Map<Integer,Double> getPercentileMap(){
//        return Quantiles.percentiles().indexes(20,75,90,99).compute(rtValues);
//    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMin_rt(int min_rt) {
        this.min_rt = min_rt;
    }

    public void setMax_rt(int max_rt) {
        this.max_rt = max_rt;
    }

    public void setAvg_rt(int avg_rt) {
        this.avg_rt = avg_rt;
    }
    
    public double getP25_rt(){
        return rt_25th;
    }

    public double getP75_rt(){
        return rt_75th;
    }

    public double getP90_rt(){
        return rt_90th;
    }

    public double getP99_rt(){
        return rt_99th;
    }
    
    public String getName() {
        return name;
    }

    public long getMin_rt() {
        return min_rt;
    }

    public long getMax_rt() {
        return max_rt;
    }

    public float getAvg_rt() {

        //如果totalCount，说明还没有任何数据，直接返回null
        if(report_total <= 0 )
            return 0;
        //avg_rt = totalTime.longValue() /totalCount.longValue();
        return (float) report_total /(float)report_account;
    }

    public float getTotalAvg_rt() {

        //如果totalCount，说明还没有任何数据，直接返回null
        if(totalTime.longValue() <= 0 )
            return 0;
        //avg_rt = totalTime.longValue() /totalCount.longValue();
        return (float) totalTime.longValue() /(float)totalCount.longValue();
    }

    public int getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(int queryCount) {
        this.queryCount = queryCount;
    }
    
    public long getTotalTime() {
        return totalTime.longValue();
    }
    

    public long getTotalCount() {
        return totalCount.longValue();
    }

    public long getErrorCount(){
        return errorCount;
    }

    public void setErrorCount(long count){
        this.errorCount = count;
    }

    public synchronized void setError(ErrorMessage errorMessage, boolean expected){
        this.errors.add(errorMessage);
        if(!expected)
            this.errorCount++;
    }

    public List<ErrorMessage> getErrors(){
        return errors;
    }

    public synchronized void increaseErrorCount(int count){
        this.errorCount += count;
    }

    public int getTps(){
        //如果end == 0，说明还没有任何数据，直接返回
        if(this.report_end == 0)
            return 0;
        this.tps = (int)((report_account*1000/(this.report_end-this.report_start)));
        return tps;
    }

    public int getTotalTps(){
        //如果end == 0，说明还没有任何数据，直接返回
        if(this.end == 0)
            return 0;
        this.tps = (int)((totalCount.longValue()*1000/(this.end-this.start)));
        return tps;
    }

    public int getQps() {
        if(this.report_end == 0)
            return 0;
        //this.tps = (int)((totalCount.longValue()*1000/(this.end-this.start)));
        this.tps = (int)((report_account*1000/(this.report_end-this.report_start)));
        this.qps = this.tps * queryCount;
        return qps;
    }

    public int getTotalQps() {
        if(this.end == 0)
            return 0;
        //this.tps = (int)((totalCount.longValue()*1000/(this.end-this.start)));
        this.tps = (int)((totalCount.longValue()*1000/(this.end-this.start)));
        this.qps = this.tps * queryCount;
        return qps;
    }
    
    

    public void setTps(int tps) {
        this.tps = tps;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public synchronized void reset(){
        this.report_start = 0;
        this.report_end = 0;
        this.report_total = 0;
        this.report_account = 0;
    }

    public void setQps(int qps) {
        this.qps = qps;
    }
    
    public void flushErrors(){
        if(this.errors.size() > 0){
            try {
                for(int i = 0; i < this.errors.size();i++){
                    error_writer.write(this.errors.get(i)+"\r\n");
                }
                error_writer.flush();
                errors.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void close(){
        try {
            error_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getVuser() {
        return vuser;
    }

    public void setVuser(int vuser) {
        this.vuser = vuser;
    }
    
    public double getSucRate(){
        BigDecimal brate = new BigDecimal((double)this.totalCount.longValue()/(double)(this.totalCount.longValue() + this.errorCount));
        return brate.setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public double getExpRate() {
        return expRate;
    }

    public void setExpRate(double expRate) {
        this.expRate = expRate;
    }

    public static void main(String[] args){
        ReentrantLock lock1 = new ReentrantLock();
        long beginTime = System.currentTimeMillis();
        ArrayList<Double> rtValues = new ArrayList<>(10000000);
        for(int i = 0; i < 10000000;i++){
            
            rtValues.add(Math.random()*10000000);
        }
        long endTime = System.currentTimeMillis();

        Runnable set = new Runnable() {
            @Override
            public void run() {
                while (true){

                    try {

                        rtValues.add(Math.random()*1000000);
                        //lock1.lockInterruptibly();
                        Thread.sleep(10);
                    //System.out.println("size  : " + rtValues.size());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }finally {
                        //lock1.unlock();
                    }
                }
            }
        };
        
        Runnable comp = new Runnable() {
            @Override
            public void run() {
                while (true){
                    long beginTime = System.currentTimeMillis();
                    Map<Integer, Double> percentile = Quantiles.percentiles().indexes(20, 75, 90, 99).compute(rtValues);
                    long endTime = System.currentTimeMillis();
                    System.out.println("comptute cost : " + (endTime - beginTime));
                }
            }
        };
        
        new Thread(set).start();
        new Thread(comp).start();
        
        
    }
    
    
}
