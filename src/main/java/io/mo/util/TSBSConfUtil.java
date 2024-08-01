package io.mo.util;

import io.mo.tsbs.DATA;

import java.io.FileNotFoundException;
import java.util.Map;

public class TSBSConfUtil {
    private static final YamlUtil ts_conf = new YamlUtil();
    private static Map conf = null;

    public static void init(){
        try {
            conf = ts_conf.getInfo("tsds.yml");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTsdsDb(){
        if(conf == null) init();
        return (String)conf.get("database");
    }

    public static int getTsdsBatchSize(){
        if(conf == null) init();
        return (int)conf.get("batchSize");
    }

    public static int getLoaderWoker(){
        if(conf == null) init();
        return (int)conf.get("loadWorker");
    }

    public static String getDDFFile(){
        if(conf == null) init();
        return (String)conf.get("ddlFile");
    }

    public static int getBufferSize(){
        if(conf == null) init();
        if(conf.get("bufferSize") == null)
            return DATA.DEFAULT_BUFFER_SIZE;
        return (int)conf.get("bufferSize");
    }
    
    public static boolean IsTimestampDefault(){
        if(conf == null) init();
        if(conf.get("defaultTimestamp") == null)
            return false;
        return (boolean)conf.get("defaultTimestamp");
    }
    
    public static int getMethod(){
        if(conf == null) init();
        if(conf.get("method") == null)
            return 0;
        return (int)conf.get("method");
    }

    public static int getInterval(){
        if(conf == null) init();
        if(conf.get("interval") == null)
            return DATA.DEFAULT_REPORT_INTERVAL;
        return (int)conf.get("interval");
    }
    
    public static String getStartTimestamp(){
        if(conf == null) init();
        if(conf.get("timestampStart") == null)
            return null;
        return (String)conf.get("timestampStart");
    }

    public static String getEndTimestamp(){
        if(conf == null) init();
        if(conf.get("timestampEnd") == null)
            return null;
        return (String)conf.get("timestampEnd");
    }

    public static void main(String[] args){
        System.out.println(getTsdsDb());
        System.out.println(IsTimestampDefault());
    }
}
