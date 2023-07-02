package io.mo.para;

import io.mo.CONFIG;
import io.mo.transaction.SQLScript;
import io.mo.transaction.Transaction;
import io.mo.util.ReplaceConfigUtil;
import org.apache.log4j.Logger;

import java.io.FileWriter;

public class IntValueQueue {

    private int read_pos = 0;//当前读的位置
    private int write_pos = 0;//当前写的位置



    private long read_time = 0;//当前读的总次数
    private long write_time = 0;//当前写的总次数

    private int paras[] = new int[CONFIG.DEFAULT_SIZE_PREPARED_PARA_PER_THREAD];

//    private FileWriter writer ;
//
//
//
//    private Transaction transaction;

    private static Logger LOG = Logger.getLogger(IntValueQueue.class);
    
//    public IntValueQueue(Transaction transaction){
//        this.transaction = transaction;
//    }

    public void add(int value){

        if(write_time - read_time > paras.length -1){
            return;
        }

        if(write_pos == paras.length){
            write_pos = 0;
            write_time++;
        }
        paras[write_pos] = value;
        write_pos ++;
        write_time++;

    }

    public void forward(int value){
        if(write_time - read_time > paras.length -1){
            LOG.debug("The trans buffer is full, do not need to supply.");
            return;
        }
//        SQLScript script = transaction.createNewScript();
//        ReplaceConfigUtil.replace(script);
        add(value);
    }

    public int getValue(){
        int value = paras[read_pos];
        if(read_pos == paras.length -1){
            read_pos = 0;
            read_time++;
        }
        else{
            read_pos++;
            read_time++;
        }
        return value;
    }

    /*
     *重新生成数据，填满队列
     */
    /*public void fill(){
        write_pos = 0;
        for(int i = 0; i < paras.length; i++){
            SQLScript script = transaction.createNewScript();
            ReplaceConfigUtil.replace(script);
            paras[i] = script;
            write_pos++;
            write_time++;
        }

        try {
            writer = new FileWriter("result/test/"+this.toString()+".sql");

            for(int i = 0; i < paras.length;i++){
                String script = transaction.getScript();
                script = ReplaceConfigUtil.replace(script);
                writer.write(script);
                paras[i] = script;
                write_pos++;
                write_time++;
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

//    public Transaction getTransaction() {
//        return transaction;
//    }
//
//    public void setTransaction(Transaction transaction) {
//        this.transaction = transaction;
//    }

    public long getRead_time() {
        return read_time;
    }

    public void setRead_time(long read_time) {
        this.read_time = read_time;
    }

    public long getWrite_time() {
        return write_time;
    }

    public void setWrite_time(long write_time) {
        this.write_time = write_time;
    }

}
