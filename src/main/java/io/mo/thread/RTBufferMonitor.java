package io.mo.thread;

import io.mo.CONFIG;
import io.mo.result.ExecResult;
import io.mo.result.RTBuffer;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;

public class RTBufferMonitor extends Thread {

    private RTBuffer buf;

    private ExecResult execResult;

    private int flush_pos = 0;
    private int time = 0;

    private static Logger LOG = Logger.getLogger(RTBufferMonitor.class.getName());

    public RTBufferMonitor(RTBuffer buf,ExecResult execResult){
        this.buf = buf;
        this.execResult = execResult;
    }


    @Override
    public void run() {
        try {
            //writer = new FileWriter("report/data/"+execResult.getName()+"-"+super.getId()+".dat");
            //String value;
            long[] tuple = null;
            while(buf.isValid()){
                //value = buf.getValue();
                tuple = buf.getRTtuple();
                while(tuple != null){
                    //writer.write(value+"\r\n");
                    //execResult.setTime(value);
                    execResult.setTime(tuple[0],tuple[1]);
                    //value = buf.getValue();
                    tuple = buf.getRTtuple();
                }
                //writer.flush();
                Thread.sleep(1000);

            }

            //执行结束，将RTBUFFER中尚未写入的数据，在一次性写入
            //value = buf.getValue();
            tuple = buf.getRTtuple();
            while(tuple != null){
                //writer.write(value+"\r\n");
                //execResult.setTime(value);
                execResult.setTime(tuple[0],tuple[1]);
                //value = buf.getValue();
                tuple = buf.getRTtuple();

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
