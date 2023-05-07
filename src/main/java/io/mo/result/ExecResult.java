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
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.math.Quantiles;
import org.checkerframework.checker.units.qual.A;

public class ExecResult {
    private String name;

    private long max_rt = -1;
    private long avg_rt = 0;
    private long min_rt = -1;
    
    private double rt_25th = -1;

    private double rt_75th = -1;
    private double rt_90th = -1;

    private double rt_99th = -1;
    
    private double expRate = 1;
    
    private ArrayList<Long> rtValues = new ArrayList<>(10000000);
    
    private ReentrantLock lock = new ReentrantLock();


    private long totalTime = 0;
    private long totalCount = 0;
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

    
    public  void setTime(long start,long end){
        try{
            lock.lockInterruptibly();
            long time = end - start;
            if(rtValues.size() < 10000000)
                rtValues.add(time);
            else {
                if (counter%10000000 == 10){
                    rtValues.set(index,time);
                }
            }

            counter++;
            if(index < rtValues.size())
                index++;
            else 
                index = 0;

            if(max_rt < 0)  max_rt = time;
            if(min_rt < 0)  min_rt = time;

            if(this.start  == 0) this.start  = start;

            if(this.end == 0)  this.end = end;

            if(max_rt < time)  max_rt = time;

            if(min_rt > time)  min_rt = time;

            if(this.start > start)  this.start = start;

            if(this.end < end) this.end = end;

            totalTime += time;
            totalCount++;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
    
     
    
    public void computePercentile(){
        Map<Integer,Double> percentile = Quantiles.percentiles().indexes(25,75,90,99).compute(rtValues);
        rt_25th = percentile.get(25);
        rt_75th = percentile.get(75);
        rt_90th = percentile.get(90);
        rt_99th = percentile.get(99);
    }
    
    public Map<Integer,Double> getPercentileMap(){
        return Quantiles.percentiles().indexes(20,75,90,99).compute(rtValues);
    }

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
        if(totalCount <= 0 )
            return 0;
        avg_rt = totalTime /totalCount;
        return (float) totalTime /(float)totalCount;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(int queryCount) {
        this.queryCount = queryCount;
    }
    
    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int count) {
        this.totalCount = count;
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
        //如果end == 0，说明还没有任何数据，直接返回0
        if(this.end == 0)
            return 0;
        this.tps = (int)((totalCount*1000/(this.end-this.start)));
        return tps;
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


    public int getQps() {
        if(this.end == 0)
            return 0;
        this.tps = (int)((totalCount*1000/(this.end-this.start)));
        this.qps = this.tps * queryCount;
        return qps;
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
        BigDecimal brate = new BigDecimal((double)this.totalCount/(double)(this.totalCount + this.errorCount));
        return brate.setScale(5,BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public double getExpRate() {
        return expRate;
    }

    public void setExpRate(double expRate) {
        this.expRate = expRate;
    }

    public static void main(String[] args){
        long beginTime = System.currentTimeMillis();
        ArrayList<Double> rtValues = new ArrayList<>(100000000);
        for(int i = 0; i < 100000000;i++){
            rtValues.add(Math.random()*100000);
        }
        long endTime = System.currentTimeMillis();
        
        System.out.println("gen data: " + (endTime - beginTime));

        beginTime = System.currentTimeMillis();
        Map<Integer,Double> percentile = Quantiles.percentiles().indexes(20,75,90,99).compute(rtValues);
        endTime = System.currentTimeMillis();
        System.out.println("compute data: " + (endTime - beginTime));
        
        System.out.println("rt_25th = " + percentile.get(20));
        System.out.println("rt_75th = " + percentile.get(75));
        System.out.println("rt_90th = " + percentile.get(90));
        System.out.println("rt_99th = " + percentile.get(99));
    }
}
