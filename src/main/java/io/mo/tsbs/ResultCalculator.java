package io.mo.tsbs;

import io.mo.CONFIG;
import io.mo.result.ErrorMessage;
import io.mo.util.TSBSConfUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultCalculator {
    public static ResultRecord[] records = new ResultRecord[DATA.DEFAULT_RESULT_BUFFER_SIZE];
    
    private static int total_metrics = 0;
    private static int total_records = 0;
    private static int totoal_transactions = 0;
    
    private int current_metrics = 0;
    private int current_records = 0;
    private int current_transactions = 0;
    
    private static int write_index = 0;
    private static int read_index = 0;
    
    private long report_interval = TSBSConfUtil.getInterval() * 1000;

    private List<ErrorMessage> errors = new ArrayList<>(DATA.DEFAULT_RESULT_BUFFER_SIZE);

    private FileWriter error_writer ;
    
    public synchronized static void addRecord(ResultRecord record){
        records[write_index] = record;
        total_metrics += record.getMetrics();
        total_records += record.getRecords();
        totoal_transactions += 1;
        if(write_index < DATA.DEFAULT_RESULT_BUFFER_SIZE -1 )
            write_index++;
        else 
            write_index = 0;
    }
    
    public String getCurrentResult(){

        current_metrics = 0;
        current_records = 0;
        
        long from_start = 0;
        long to_start = 0;
        
        long current_start = 0;
        long current_end = 0;
        
        if(records[read_index] != null){
            from_start = records[read_index].getStart();
            to_start = records[read_index].getStart();

            current_start = records[read_index].getStart();
            current_end = records[read_index].getEnd();
            
            current_metrics += records[read_index].getMetrics();
            current_records += records[read_index].getRecords();
            current_transactions += 1;
            records[read_index] = null;
            read_index++;
        }
        
        while(records[read_index] != null){
            to_start = records[read_index].getStart();
            if(to_start < from_start) {
                from_start = to_start + from_start;
                to_start = from_start - to_start;
                from_start = from_start - to_start;
            }
            if(to_start - from_start > report_interval)
                break;
            else {
                
                if(current_start > records[read_index].getStart())
                    current_start = records[read_index].getStart();
                
                if(current_end < records[read_index].getEnd())
                    current_end = records[read_index].getEnd();
                
                current_metrics += records[read_index].getMetrics();
                current_records += records[read_index].getRecords();
                current_transactions += 1;
                records[read_index] = null;
                read_index++;
            }
        }
        
        long real_interval =current_end - current_start;
        if(real_interval < report_interval)
            real_interval = report_interval;
        
        return String.format("metrics(i/t): %d/%d, records(i/t): %d/%d, transactions(i/t): %d/%d, m/s: %.2f, r/s: %.2f, t/s: %.2f",
                current_metrics,total_metrics,current_records,total_records,current_transactions,totoal_transactions, 
                (long)current_metrics/real_interval,(long)current_records/real_interval,(long)current_transactions/real_interval);
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
    
}
