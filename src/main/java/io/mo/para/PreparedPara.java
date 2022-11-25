package io.mo.para;

import io.mo.CONFIG;
import io.mo.util.ReplaceConfigUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PreparedPara {
    private String type;


    private String org_value;
    public Queue<String> str_values = new ConcurrentLinkedQueue<String>();
    public Queue<Integer> int_values = new ConcurrentLinkedQueue<Integer>();

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



    public PreparedPara(String type,String org_value){
        this.type = type;
        this.org_value = org_value;
        if(this.type.equalsIgnoreCase("INT")) {
            for (int i = 0; i < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD; i++)
                int_values.add(Integer.parseInt(ReplaceConfigUtil.replace(org_value)));
        }

        if(this.type.equalsIgnoreCase("STR")) {
            for (int i = 0; i < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD; i++)
                str_values.add(ReplaceConfigUtil.replace(org_value));
        }

        //Producer producer = new Producer();
        //producer.start();
    }

    public int getIntValue(){
        return int_values.poll();
    }

    public String getStrValue(){
        return str_values.poll();
    }

    private class Producer extends Thread{
        public void run(){
            while(!CONFIG.TIMEOUT){
                if(type.equalsIgnoreCase("INT")){
                    if(int_values.size() < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD){

                        int_values.add(Integer.parseInt(ReplaceConfigUtil.replace(org_value)));
                    }
                }

                if(type.equalsIgnoreCase("STR")){
                    if(str_values.size() < CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD){

                        str_values.add(ReplaceConfigUtil.replace(org_value));
                    }
                }
            }
        }
    }

    public boolean isINT(){
        return this.type.equalsIgnoreCase("INT");
    }

    public boolean isSTR(){
        return this.type.equalsIgnoreCase("STR");
    }

}

