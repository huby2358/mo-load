package io.mo.util;

import io.mo.CONFIG;
import io.mo.MOPerfTest;
import io.mo.transaction.PreparedSQLCommand;
import io.mo.transaction.SQLScript;
import io.mo.transaction.Transaction;
import io.mo.transaction.ts.TSSQLScript;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RunConfigUtil {
    private static YamlUtil transaction = new YamlUtil();
    private static Map map = null;

    private static List<Transaction> transactions = new ArrayList<>();
    private static Logger LOG = Logger.getLogger(RunConfigUtil.class.getName());

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            String runYml = System.getProperty("run.yml");
            if(runYml == null)
                runYml = "run.yml";
            
            map = transaction.getInfo(runYml);
            List transT = (List) map.get("transaction");
            
            for(int i = 0; i < transT.size();i++){
                Map transM = (Map)transT.get(i);
                String name = (String)transM.get("name");
                int vuser = (int)transM.get("vuser");
                
                String type = (String)transM.get("type");
                if(type == null){
                    type = CONFIG.TXN_TYPE_COMMON;
                }
                
                Transaction trans = new Transaction(name,vuser);
                trans.setType(type);
                
                if(transM.get("wait") != null) {
                    int waitTime = (int) transM.get("wait");
                    trans.setWaitTime(waitTime);
                }

                if(transM.get("interval") != null) {
                    int interval = (int) transM.get("interval");
                    trans.setInterval(interval);
                }
                
                switch (type){
                    case CONFIG.TXN_TYPE_COMMON:
                        if(null != transM.get("mode")){
                            int mode = (int)transM.get("mode");
                            trans.setMode(mode);
                            LOG.info("transaction["+trans.getName()+"].mode = "+mode);
                        }

                        if(null != transM.get("prepared")){
                            String prepared = (String)transM.get("prepared");
                            if(prepared.equalsIgnoreCase("true"))
                                trans.setPrepared(true);
                            LOG.info("transaction["+trans.getName()+"].prepared = "+prepared);
                        }

                        if(null != transM.get("sucrate")){
                            Object sucrate = transM.get("sucrate");
                            if (sucrate instanceof Integer)
                                trans.setSucrate(((Integer) sucrate).doubleValue());
                            if (sucrate instanceof String)
                                trans.setSucrate(Double.valueOf((String) sucrate));

                            if (sucrate instanceof Double)
                                trans.setSucrate((Double) sucrate);
                            LOG.info("transaction["+trans.getName()+"].sucrate = "+sucrate);
                        }

                        if(null != transM.get("accept")){
                            String acceptErrorcodes  = String.valueOf(transM.get("accept"));
                            String[] acceptErrorcodeArray = acceptErrorcodes.split(",");
                            for (String errorCode: acceptErrorcodeArray)
                                trans.addAcceptErrorCodes(Integer.valueOf(errorCode));
                            LOG.info("transaction["+trans.getName()+"].accept = " + Arrays.toString(acceptErrorcodeArray));
                        }

                        List sqls = (List)transM.get("script");
                        SQLScript script = new SQLScript(sqls.size());

                        trans.setScript(script);

                        for(int j = 0;j < sqls.size();j++){
                            if(!trans.isPrepared()) {
                                Map sqlM = (Map) sqls.get(j);
                                String sql = (String) sqlM.get("sql");
                                script.addCommand(sql);
                                //trans.setScript(sql);
                            }else {
                                Map sqlM = (Map) sqls.get(j);
                                String sql = (String) sqlM.get("sql");
                                String paras = (String) sqlM.get("paras");
                                PreparedSQLCommand command = new PreparedSQLCommand(sql);
                                command.parseParas(paras);
                                script.addPreparedCommand(command);
                            }

                        }
                        
                        break;
                        
                    case CONFIG.TXN_TYPE_TS:
                        trans.setMode(0);
                        trans.setPrepared(false);
                        int batch_size = CONFIG.TS_DEFAULT_BATCH_SIZE;
                        if(null != transM.get("batch_size")){
                            batch_size = (int)transM.get("batch_size");
                            LOG.info("transaction["+trans.getName()+"].batch_size = " + batch_size);
                        }

                        String table_name = CONFIG.TS_DEFAULT_TABLE_NAME;
                        if(null != transM.get("table_name")){
                            table_name = (String) transM.get("table_name");
                            LOG.info("transaction["+trans.getName()+"].table_name = " + table_name);
                        }

                        String method = CONFIG.TS_DEFAULT_LOAD_METHOD;
                        if(null != transM.get("method")){
                            method = (String) transM.get("method");
                            LOG.info("transaction["+trans.getName()+"].method = " + method);
                        }
                        
                        String device = (String) transM.get("device");
                        if(device == null){
                            LOG.error(String.format("The para[device] in transaction[%s] can not be null,please check..",trans.getName()));
                            System.exit(1);
                        }

                        String metric = (String) transM.get("metric");
                        if(metric == null){
                            LOG.error(String.format("The para[metric] in transaction[%s] can not be null,please check..",trans.getName()));
                            System.exit(1);
                        }

                        String  field_separator = (String) transM.get("field_separator");
                        String  from = (String) transM.get("from");    
                        
                        TSSQLScript tsScript = new TSSQLScript(table_name,device,metric,batch_size);
                        //System.out.println(tsScript.getMethod());
                        tsScript.setMethod(method);

                        if(field_separator == null){
                            LOG.warn(String.format("The para[field_separator] in transaction[%s] is null, using default value[|]",trans.getName()));
                        }else
                            tsScript.setField_separator(field_separator);
                        
                        if(from != null){
                            tsScript.STARTTIME.setTime(format.parse(from));
                        }
                        
                        trans.setScript(tsScript);
                        
                        break;
                    default:
                        break;
                }
                
                transactions.add(trans);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Transaction getTransaction(int i){
        return transactions.get(i);
    }

    public static int getTransactionNum(){
        return transactions.size();
    }

    public static long getExecDuration() { return (int)map.get("duration"); }
    
    public static String getStdout(){
        return (String)map.get("stdout");
    }

    public static void main(String args[]){
        for(int i = 0; i < RunConfigUtil.getTransactionNum(); i++){
            Transaction t = RunConfigUtil.getTransaction(i);
            System.out.println(t.getName());
            System.out.println(t.getTheadnum());
            System.out.println(t.getScript());
            System.out.println(RunConfigUtil.getExecDuration());
        }
    }
}