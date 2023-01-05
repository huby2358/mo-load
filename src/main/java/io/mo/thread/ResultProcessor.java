package io.mo.thread;

import io.mo.CONFIG;
import io.mo.MOPerfTest;
import io.mo.result.ExecResult;
import io.mo.util.RunConfigUtil;
import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResultProcessor extends Thread{

    private List<ExecResult> results = new ArrayList<>();

    private int turn = 0;

    private String cell_top = "-----------------";
    private String cell_blk = "                 ";

    private FileWriter error_writer;
    private FileWriter summary_writer;
    private FileWriter[] trans_writer;
    private StringBuffer summary;
    
    private String stdout =null;

    private static Logger LOG = Logger.getLogger(ResultProcessor.class.getName());

    public ResultProcessor(){
        summary  = new StringBuffer();
        File res_dir = new File("report/");
        if(!res_dir.exists())
            res_dir.mkdirs();
        try {
            summary_writer = new FileWriter("report/summary.txt");
            //result_writer  = new FileWriter("report/result.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        stdout = RunConfigUtil.getStdout();
    }

    public void addResult(ExecResult execResult){
        results.add(execResult);
        
    }


    @Override
    public void run() {
        trans_writer = new FileWriter[results.size()];
        for(int i = 0; i < results.size(); i++){
            try {
                trans_writer[i] = new FileWriter("report/" + results.get(i).getName() +".data");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
            
        System.out.println(getTitle());
        
        String[] objs = new String[results.size()*2];
        while (true){
            String format = "";
            //ExecResult execResult = results.get(turn);
            if(isTerminated()){
                for(int i = 0;i < results.size();i++){
                    format += "%s\n";
                    objs[i*2] = getResult(i);
                    format += "%s\n";
                    objs[i*2+1] = "|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+cell_top +"|"+ cell_top +"|";
                    try {
                        Date now = new Date();
                        trans_writer[i].write("["+now+"] "+ objs[i*2].substring(1).replaceAll(" ","").replaceAll("\\|",",")+"\n");
                        trans_writer[i].flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                format += "\33["+(objs.length+1)+"A\r\n";

                System.out.printf(format, objs);
                System.out.printf("\033[" + objs.length + "B\r\n");

                try {
                    summary_writer.write(getSummary());
                    summary_writer.flush();
                    summary_writer.close();
                    LOG.info("\n"+getSummary());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }

            for(int i = 0;i < results.size();i++){
                format += "%s\n";
                objs[i*2] = getResult(i);
                format += "%s\n";
                objs[i*2+1] = "|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+cell_top +"|"+ cell_top +"|";
                try {
                    Date now = new Date();
                    trans_writer[i].write("["+now+"] "+ objs[i*2].substring(1).replaceAll(" ","").replaceAll("\\|",",")+"\n");
                    trans_writer[i].flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            format += "\033["+(objs.length+1)+"A\r\n";

            System.out.printf(format,objs);

            for(int i = 0; i < results.size();i++){
                results.get(i).flushErrors();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        for(int i = 0; i < trans_writer.length; i++){
            try {
                trans_writer[i].close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void print(){
        System.out.println(getTitle());
    }

    public String getTitle(){
        String TRANSNAME    = "    TRANSNAME    ";
        String RT_MAX       = "      RT_MAX     ";
        String RT_MIN       = "      RT_MIN     ";
        String RT_AVG       = "      RT_AVG     ";
        String TPS          = "       TPS       ";
        String QPS          = "       QPS       ";
        String SUCCESS      = "      SUCCESS    ";
        //String TOTAL        = "      TOTAL      ";
        String ERROR        = "      ERROR      ";

        return  "|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+cell_top +"|"+ cell_top +"|"+ cell_top +"|\r\n"+
                "|"+TRANSNAME+"|"+RT_MAX+"|"+RT_MIN+"|"+RT_AVG+"|"+TPS+"|"+QPS+"|"+SUCCESS+"|"+ERROR+"|\r\n"+
                "|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+ cell_top +"|"+cell_top +"|"+ cell_top +"|"+ cell_top +"|";
    }

    public String getResult(int turn){
        ExecResult execResult = results.get(turn);
        String name = formatName(execResult.getName());
        
        String TRANSNAME = cell_blk.substring(0,2)+name+ cell_blk.substring(2+name.length(), cell_blk.length());
        String RT_MAX    = cell_blk.substring(0,6)+execResult.getMax_rt()+ cell_blk.substring(6+String.valueOf(execResult.getMax_rt()).length(), cell_blk.length());
        String RT_MIN    = cell_blk.substring(0,6)+execResult.getMin_rt()+ cell_blk.substring(6+String.valueOf(execResult.getMin_rt()).length(), cell_blk.length());
        String RT_AVG    = cell_blk.substring(0,6)+execResult.getAvg_rt()+ cell_blk.substring(6+String.valueOf(execResult.getAvg_rt()).length(), cell_blk.length());
        String TPS       = cell_blk.substring(0,7)+execResult.getTps()+ cell_blk.substring(7+String.valueOf(execResult.getTps()).length(), cell_blk.length());
        String QPS       = cell_blk.substring(0,7)+execResult.getQps()+ cell_blk.substring(7+String.valueOf(execResult.getQps()).length(), cell_blk.length());
        String SUCCESS     = cell_blk.substring(0,6)+execResult.getTotalCount()+ cell_blk.substring(6+String.valueOf(execResult.getTotalCount()).length(), cell_blk.length());
        String ERROR     = cell_blk.substring(0,6)+execResult.getErrorCount()+ cell_blk.substring(6+String.valueOf(execResult.getErrorCount()).length(), cell_blk.length());


        return "|"+TRANSNAME+"|"+RT_MAX+"|"+RT_MIN+"|"+RT_AVG+"|"+TPS+"|"+QPS+"|"+SUCCESS+"|"+ERROR+"|";
    }
    
    public String getResultData(int turn){
        ExecResult execResult = results.get(turn);
        String TRANSNAME = execResult.getName();
        long RT_MAX    = execResult.getMax_rt();
        long RT_MIN    = execResult.getMin_rt();
        String RT_AVG    = execResult.getAvg_rt();
        int TPS       = execResult.getTps();
        int QPS       = execResult.getQps();
        long SUCCESS     = execResult.getTotalCount();
        long ERROR     = execResult.getErrorCount();


        return TRANSNAME+","+RT_MAX+","+RT_MIN+","+RT_AVG+","+TPS+","+QPS+","+SUCCESS+","+ERROR;
    }

    public String getSummary(){
        summary.delete(0,summary.length());
        for(int i = 0;i < results.size();i++){
            ExecResult execResult = results.get(i);
            summary.append("["+execResult.getName()+"]\n");
            summary.append("RT_MAX : " + execResult.getMax_rt()+"\n");
            summary.append("RT_MIN : " + execResult.getMin_rt()+"\n");
            summary.append("RT_AVG : " + execResult.getAvg_rt()+"\n");
            summary.append("TPS : " + execResult.getTps()+"\n");
            summary.append("QPS : " + execResult.getQps()+"\n");
            summary.append("SUCCESS : " + execResult.getTotalCount()+"\n");
            summary.append("ERROR : " + execResult.getErrorCount()+"\n");
            summary.append("\n");
        }
        return summary.toString();
    }

    public boolean isTerminated(){

        for(int i = 0;i < results.size();i++){
            ExecResult result = results.get(i);
            if(result.getThreadnum() != 0)
                return false;
        }
        CONFIG.TIMEOUT = true;
        return true;
    }
    
    public String formatName(String name){
        if(name.length() <= 12)
            return name;
        String prefix = name.substring(0,5);
        String postfix = name.substring(name.length()-5,name.length());
        return prefix + "**" + postfix;
    } 


    public static void main(String args[]){
        ExecResult e = new ExecResult("point_select_prepare");
        ResultProcessor r = new ResultProcessor();
        System.out.println(r.formatName("point_select_prepare"));
        

    }
}
