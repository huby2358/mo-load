package io.mo.result;

import io.mo.CONFIG;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExecResult {
    private String name;

    private long max_rt = -1;
    private long avg_rt = 0;
    private long min_rt = -1;


    private long totalTime = 0;
    private long totalCount = 0;
    private long errorCount = 0;

    private int tps = 0;

    private long start = 0;

    private long end = 0;

    public int getThreadnum() {
        return threadnum;
    }

    public synchronized void increaseThread() {
        this.threadnum++;
    }

    public synchronized void decreaseThread() {
        this.threadnum--;
    }

    private int threadnum = 0;


    private List<String> errors = new ArrayList<String>(CONFIG.TEMP_ERROR_BUF_SIZE);


    private  FileWriter error_writer ;


    private boolean terminated = false;

    public ExecResult(String name){
        this.name = name;
        try {
            error_writer = new FileWriter("report/error/" + name + ".err");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setTime(String time){
        String[] temp = time.split("=");
        String[] period = temp[1].split(":");
        long start = Long.parseLong(period[0]);
        long end = Long.parseLong(period[1]);
        setTime(start,end);
    }

    public synchronized void setTime(long start,long end){
        long time = end - start;

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


    public String getName() {
        return name;
    }

    public long getMin_rt() {
        return min_rt;
    }

    public long getMax_rt() {
        return max_rt;
    }

    public String getAvg_rt() {

        //如果totalCount，说明还没有任何数据，直接返回null
        if(totalCount <= 0 )
            return null;
        avg_rt = totalTime /totalCount;
        return String.format("%.2f",(float) totalTime /(float)totalCount);
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

    public synchronized void setError(String error){
        this.errors.add(error);
        this.errorCount++;
    }

    public List<String> getErrors(){
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
}
