package io.mo.para;

import io.mo.CONFIG;
import io.mo.MOPerfTest;
import io.mo.replace.PreparedVariable;
import io.mo.replace.Variable;
import io.mo.util.ReplaceConfigUtil;
import org.apache.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PreparedPara {
    private String type;


    private String org_value;
    public StrValueQueue str_values = new StrValueQueue();
    public IntValueQueue int_values = new IntValueQueue();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrg_value() {
        return org_value;
    }

    public void setOrg_value(String org_value) {
        this.org_value = org_value;
    }

    private static Logger LOG = Logger.getLogger(PreparedPara.class.getName());


    public PreparedPara(String type,String org_value){
        this.type = type;
        this.org_value = org_value;
        if(this.type.equalsIgnoreCase("INT")) {
            for (int i = 0; i < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD; i++)
                int_values.add(Integer.parseInt(replace(org_value)));
        }

        if(this.type.equalsIgnoreCase("STR")) {
            for (int i = 0; i < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD; i++)
                str_values.add(replace(org_value));
        }

        //Producer producer = new Producer();
        //producer.start();
    }

    public int getIntValue(){
        
        return int_values.getValue();
    }

    public String getStrValue(){
        return str_values.getValue();
    }

//    private class Producer extends Thread{
//        public void run(){
//            while(!CONFIG.TIMEOUT){
//                System.out.println("int_values.size() = " + int_values);
//                if(type.equalsIgnoreCase("INT")){
//                    if(int_values.size() < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD){
//
//                        int_values.add(Integer.parseInt(ReplaceConfigUtil.replace(org_value)));
//                    }
//                }
//
//                if(type.equalsIgnoreCase("STR")){
//                    if(str_values.size() < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD){
//
//                        str_values.add(ReplaceConfigUtil.replace(org_value));
//                    }
//                }
//            }
//        }
//    }

    public boolean isINT(){
        return this.type.equalsIgnoreCase("INT");
    }

    public boolean isSTR(){
        return this.type.equalsIgnoreCase("STR");
    }

    public  String replace(String str){
        for(int i = 0;i < ReplaceConfigUtil.vars.size();i++) {
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            if(str.indexOf("{"+var.getName()+"}") != -1)
                str = str.replaceAll("\\{"+var.getName()+"\\}",var.nextValue());
        }
        str = PreparedVariable.replace(str);
        return str;
    }

}

