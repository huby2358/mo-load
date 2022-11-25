package io.mo.util;

import io.mo.MOPerfTest;
import io.mo.transaction.SQLScript;
import io.mo.transaction.Transaction;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RunConfigUtil {
    private static YamlUtil transaction = new YamlUtil();
    private static Map map = null;

    private static List<Transaction> transactions = new ArrayList<>();
    private static Logger LOG = Logger.getLogger(RunConfigUtil.class.getName());

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

                Transaction trans = new Transaction(name,vuser);


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

                if(null != transM.get("paras")){
                    String paras = (String)transM.get("paras");
                    trans.setParas(paras);
                    LOG.info("transaction["+trans.getName()+"].paras = "+paras);
                }

                List sqls = (List)transM.get("script");
                SQLScript script = new SQLScript(sqls.size());

                trans.setScript(script);

                for(int j = 0;j < sqls.size();j++){
                    Map sqlM = (Map)sqls.get(j);
                    String sql = (String) sqlM.get("sql");
                    script.addCommand(sql);
                    //trans.setScript(sql);
                }
                transactions.add(trans);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Transaction getTransaction(int i){
        return transactions.get(i);
    }

    public static int getTransactionNum(){
        return transactions.size();
    }

    public static long getExecDuration() { return (int)map.get("duration"); }
    
    public static String getStdout(){return (String)map.get("stdout");}

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