package io.mo.util;

import io.mo.sysbench.Sysbench;

import java.io.FileNotFoundException;
import java.util.Map;

public class SysbenchConfUtil {
    private static final YamlUtil sys_conf = new YamlUtil();
    private static Map conf = null;

    public static void init(){
        try {
            conf = sys_conf.getInfo("sysbench.yml");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSysbenchDb(){
        if(conf == null) init();
        return (String)conf.get("database");
    }

    public static String getSysbenchTablePrefix(){
        if(conf == null) init();
        return (String)conf.get("tableName");
    }

    public static int getSysbenchTableCount(){
        if(conf == null) init();
        return (int)conf.get("tableCount");
    }

    public static int getSysbenchTableSize(){
        if(conf == null) init();
        return (int)conf.get("tableSize");
    }

    public static String getSysbenchAutoIncrement(){
        if(conf == null) init();
        return (String)conf.get("autoIncrement");
    }

    public static int getSysbenchBatchSize(){
        if(conf == null) init();
        return (int)conf.get("batchSize");
    }

    public static void main(String[] args){
        System.out.println(getSysbenchDb());
        System.out.println(getSysbenchTableSize());
        System.out.println(Sysbench.getRandomChar(120));
    }
}
