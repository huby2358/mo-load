package io.mo.thread;

import io.mo.CONFIG;
import io.mo.para.PreparedPara;
import io.mo.replace.PreparedVariable;
import io.mo.replace.Variable;
import io.mo.transaction.PreparedSQLCommand;
import io.mo.util.ReplaceConfigUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PreparedParaProducer extends Thread{
    public List<PreparedPara> getBuffers() {
        return buffers;
    }

    public void setBuffers(List<PreparedPara> buffers) {
        this.buffers = buffers;
    }

    private List<PreparedPara> buffers = new ArrayList<PreparedPara>();

    private static Logger LOG = Logger.getLogger(PreparedSQLCommand.class.getName());
    
    public void addPreparedPara(PreparedPara para){
        buffers.add(para);
    }

    public void run(){
        while(!CONFIG.TIMEOUT){
            for(int i = 0; i < buffers.size(); i++){
                PreparedPara para = buffers.get(i);
                //System.out.println("para.int_values = " +i+": "+ para.int_values.size());
                //System.out.println("int_values.size() = " + para.int_values.size());
                
                if(para.getType().equalsIgnoreCase("INT")){
//                    int left = para.int_values();
//                    if(left < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD){
//                        for(int j = 0; j < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD - left; j++) {
//                            para.int_values.add(Integer.parseInt(ReplaceConfigUtil.replace(para.getOrg_value())));
//                        }
//                    }
                    para.int_values.forward(Integer.parseInt(replace(para.getOrg_value())));
                }

                if(para.getType().equalsIgnoreCase("STR")){
//                    int left = para.str_values.size();
//                    if(para.str_values.size() < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD){
//                        for(int j = 0; j < CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD - left; j++)
//                            para.str_values.add(ReplaceConfigUtil.replace(para.getOrg_value()));
//                    }
                    para.str_values.forward(replace(para.getOrg_value()));
                }
            }

        }
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
