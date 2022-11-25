package io.mo.thread;

import io.mo.CONFIG;
import io.mo.result.ExecResult;
import io.mo.result.RTBuffer;

import java.io.FileWriter;
import java.io.IOException;

public class RTBufferMonitor extends Thread {

    private RTBuffer buf;

    private ExecResult execResult;

    private int flush_pos = 0;
    private int time = 0;

    private FileWriter writer ;

    public RTBufferMonitor(RTBuffer buf,ExecResult execResult){
        this.buf = buf;
        this.execResult = execResult;
    }


    @Override
    public void run() {
        try {
            //writer = new FileWriter("report/data/"+execResult.getName()+"-"+super.getId()+".dat");
            String value;
            while(buf.isValid()){
                value = buf.getValue();
                while(value != null){
                    //writer.write(value+"\r\n");
                    execResult.setTime(value);
                    value = buf.getValue();
                }
                //writer.flush();
                Thread.sleep(1000);

            }

            //执行结束，将RTBUFFER中尚未写入的数据，在一次性写入
            value = buf.getValue();
            while(value != null){
                //writer.write(value+"\r\n");
                execResult.setTime(value);
                value = buf.getValue();

            }

            execResult.decreaseThread();

            //writer.flush();
            //writer.close();



        }/*catch (IOException e) {
            e.printStackTrace();
        }*/ catch (InterruptedException e) {
            e.printStackTrace();
        } 
    }

}
