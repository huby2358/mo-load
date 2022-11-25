package io.mo.transaction;

import io.mo.CONFIG;
import io.mo.util.ReplaceConfigUtil;

import java.io.FileWriter;

public class TransBuffer {

    private int read_pos = 0;//当前读的位置
    private int write_pos = 0;//当前写的位置



    private long read_time = 0;//当前读的总次数
    private long write_time = 0;//当前写的总次数

    private SQLScript scripts[] = new SQLScript[CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD];

    private FileWriter writer ;



    private Transaction transaction;

    public TransBuffer(Transaction transaction){
        this.transaction = transaction;
    }

    public void addScript(SQLScript script){

        if(write_time - read_time > scripts.length -1){
            return;
        }

        if(write_pos == scripts.length){
            write_pos = 0;
            write_time++;
        }
        scripts[write_pos] = script;
        write_pos ++;
        write_time++;

    }

    public void forward(){
        if(write_time - read_time > scripts.length -1){
            return;
        }
        SQLScript script = transaction.createNewScript();
        ReplaceConfigUtil.replace(script);
        addScript(script);
    }

    public SQLScript getScript(){
        SQLScript script = scripts[read_pos];
        if(read_pos == scripts.length -1){
            read_pos = 0;
            read_time++;
        }
        else{
            read_pos++;
            read_time++;
        }

        return script;
    }

    /*
     *重新生成数据，填满队列
     */
    public void fill(){
        write_pos = 0;
        for(int i = 0; i < scripts.length;i++){
            SQLScript script = transaction.createNewScript();
            ReplaceConfigUtil.replace(script);
            scripts[i] = script;
            write_pos++;
            write_time++;
        }

        /*try {
            writer = new FileWriter("result/test/"+this.toString()+".sql");

            for(int i = 0; i < scripts.length;i++){
                String script = transaction.getScript();
                script = ReplaceConfigUtil.replace(script);
                writer.write(script);
                scripts[i] = script;
                write_pos++;
                write_time++;
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

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
