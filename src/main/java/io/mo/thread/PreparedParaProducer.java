package io.mo.thread;

import io.mo.CONFIG;
import io.mo.para.PreparedPara;
import io.mo.util.ReplaceConfigUtil;

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

    public void addPreparedPara(PreparedPara para){
        buffers.add(para);
    }

    public void run(){
        while(!CONFIG.TIMEOUT){
            for(int i = 0; i < buffers.size(); i++){
                PreparedPara para = buffers.get(i);
                if(para.getType().equalsIgnoreCase("INT")){
                    if(para.int_values.size() < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD){

                        para.int_values.add(Integer.parseInt(ReplaceConfigUtil.replace(para.getOrg_value())));
                    }
                }

                if(para.getType().equalsIgnoreCase("STR")){
                    if(para.str_values.size() < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD){

                        para.str_values.add(ReplaceConfigUtil.replace(para.getOrg_value()));
                    }
                }
            }

        }
    }
}
