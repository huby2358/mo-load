package io.mo.transaction;

import io.mo.para.PreparedPara;
import io.mo.thread.PreparedParaProducer;

public class PreparedSQLCommand {
    private String sql = null;

    
    private PreparedPara[] preparedParas;


    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }


    public PreparedSQLCommand(String sql){
        this.sql = sql;
    }

    public PreparedPara[] getPreparedParas() {
        return preparedParas;
    }

    public void setPreparedParas(PreparedPara[] preparedParas) {
        this.preparedParas = preparedParas;
    }


    public void parseParas(String paras){
        if(paras == null){
            preparedParas = new PreparedPara[0];
            return;
        }
        
        String[] array = paras.split(",");
        preparedParas = new PreparedPara[array.length];
        for(int i = 0;i < array.length;i++){
            if(array[i].startsWith("INT")){
                PreparedPara para = new PreparedPara("INT",array[i].substring(4,array[i].length()-1));

                preparedParas[i] = para;
            }

            if(array[i].startsWith("STR")){
                PreparedPara para = new PreparedPara("STR",array[i].substring(4,array[i].length()-1));
                preparedParas[i] = para;
            }
        }
    }
}
