package io.mo.util;

import io.mo.CONFIG;
import io.mo.MOPerfTest;
import io.mo.replace.*;
import io.mo.transaction.SQLScript;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceConfigUtil {
    private static YamlUtil replace = new YamlUtil();
    public static List<Variable> vars = new ArrayList<>();
    private static Logger LOG = Logger.getLogger(MOPerfTest.class.getName());

    static {
        try {
            String replaceYml = System.getProperty("replace.yml");
            if(replaceYml == null)
                replaceYml = "replace.yml";
            
            Map param = replace.getInfo(replaceYml);
            List varsT = new ArrayList();

            if(null != param.get("replace"))
                varsT = (List) param.get("replace");

            for(int i = 0;i < varsT.size();i++){
                Map var = (Map)varsT.get(i);
                if(var.get("type").equals(CustomizedReplaceType.SEQUENCE)){
                    SequenceVariable seq = new SequenceVariable(var.get("name").toString(),Long.parseLong(var.get("start").toString()));
                    seq.setStep(Integer.parseInt(var.get("step").toString()));
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        seq.setScope(scope);
                    }
                    vars.add(seq);
                }

                if(var.get("type").equals(CustomizedReplaceType.RANDOM)){
                    RandomVariable ran = new RandomVariable(var.get("name").toString(),var.get("range").toString());
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        ran.setScope(scope);
                    }
                    vars.add(ran);
                }

                if(var.get("type").equals(CustomizedReplaceType.RBIGNIT)){
                    RandomBigintVariable ran = new RandomBigintVariable(var.get("name").toString(),var.get("range").toString());
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        ran.setScope(scope);
                    }
                    vars.add(ran);
                }

                if(var.get("type").equals(CustomizedReplaceType.ENUM)){
                    EnumVariable ran = new EnumVariable(var.get("name").toString(),var.get("value").toString());
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        ran.setScope(scope);
                    }
                    vars.add(ran);
                }

                if(var.get("type").equals(CustomizedReplaceType.DBRS)){
                    
                    String name = (String) var.get("name");
                    String sql = (String) var.get("sql");
                    if(sql == null || sql.equalsIgnoreCase("")){
                        LOG.error(String.format("Parameter[sql] can not be null in variable[%s], please check...",name));
                        System.exit(1);
                    }

                    DBResultSetVariable ran = new DBResultSetVariable(name,sql);


                    if(var.get("refresh") != null){
                        int refresh = Integer.parseInt(var.get("refresh").toString());
                        ran.setRefresh(refresh);
                    }
                    
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        ran.setScope(scope);
                    }
                    
                    ran.init();
                    vars.add(ran);
                }

                if(var.get("type").equals(CustomizedReplaceType.FILE)){
                    FileVariable file = new FileVariable(var.get("name").toString(),var.get("path").toString());
                    if(var.get("scope") != null){
                        int scope = Integer.parseInt(var.get("scope").toString());
                        file.setScope(scope);
                    }
                    vars.add(file);
                }
            }
        } catch (FileNotFoundException e){
            //e.printStackTrace();
            LOG.info("No variable config file was found.");
        }


    }

    public static boolean exists(String var){
        return vars.contains(var);
    }

    public synchronized static String replace(String str){
        for(int i = 0;i < vars.size();i++) {
            Variable var = (Variable) vars.get(i);
            if(str.indexOf("{"+var.getName()+"}") != -1)
                str = str.replaceAll("\\{"+var.getName()+"\\}",var.nextValue());
        }
        str = PreparedVariable.replace(str);
        return str;
    }

    public synchronized static void replace(SQLScript script){
        for(int i = 0;i < vars.size();i++) {
            Variable var = (Variable) vars.get(i);
            if(script.indexOf("{"+var.getName()+"}") != -1) {
                if(var.getScope() == CONFIG.PARA_SCOPE_TRANSCATION) {
                    script.replaceAll("\\{" + var.getName() + "\\}", var.nextValue());
                    continue;
                }
                
                if(var.getScope() == CONFIG.PARA_SCOPE_STATEMENT){
                    script.replaceAll("\\{" + var.getName() + "\\}", var);
                }
            }
        }

        PreparedVariable.replace(script);
    }


    public static void main(String[] args){
        System.out.println("Now start to init the variables,please wait.............");
        for(int i = 0; i < ReplaceConfigUtil.vars.size(); i++){
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            var.init();
        }
        System.out.println("Tthe variables has been prepared!");

        for(int i = 0; i < ReplaceConfigUtil.vars.size();i++){
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            if(var.getName().equalsIgnoreCase("sequence")){
                for(int j = 0; j < 10; j++){
                    System.out.println("SEQUENCE["+j+"] = "+var.nextValue());
                }
            }

            if(var.getName().equalsIgnoreCase("random")){
                for(int j = 0; j < 10; j++){
                    System.out.println("RANDOM["+j+"] = "+var.nextValue());
                }
            }
        }

    }

}
