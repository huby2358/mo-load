package io.mo.tsbs;

public class ResultRecord {
    private long start = 0;
    private long end = 0;
    private int metrics = 0;
    private int records = 0;
    
    public ResultRecord(){
        
    }
    
    public ResultRecord(long start, long end, int metrics, int records){
        this.start = start;
        this.end = end;
        this.metrics = metrics;
        this.records = records;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getMetrics() {
        return metrics;
    }

    public void setMetrics(int metrics) {
        this.metrics = metrics;
    }

    public int getRecords() {
        return records;
    }

    public void setRecords(int records) {
        this.records = records;
    }
}
