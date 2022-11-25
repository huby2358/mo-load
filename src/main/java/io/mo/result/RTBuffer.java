package io.mo.result;

import io.mo.CONFIG;
import io.mo.thread.RTBufferMonitor;

import java.io.FileWriter;

public class RTBuffer {

    public String[] rts = new String[CONFIG.TEMP_RT_BUF_SIZE_PER_THREAD];

    private RTBufferMonitor monitor;

    private ExecResult execResult;

    private int write_pos = 0;

    private int read_pos = 0;

    private FileWriter writer ;

    private boolean valid = true;

    public RTBuffer(ExecResult execResult){
        this.execResult = execResult;
        monitor = new RTBufferMonitor(this,this.execResult);
        monitor.start();
    };

    public void setValue(String value){
        rts[write_pos] = value;
        write_pos ++;
        if(write_pos == CONFIG.TEMP_RT_BUF_SIZE_PER_THREAD)
            write_pos = 0;
    }

    public String getValue(){
        int temp = write_pos;
        //当前buffer中的数据已经被完全读取
        if(read_pos == temp)
            return null;

        String value = rts[read_pos];
        read_pos++;
        if(read_pos == CONFIG.TEMP_RT_BUF_SIZE_PER_THREAD)
            read_pos = 0;

        return value;
    }



    public RTBufferMonitor getMonitor() {
        return monitor;
    }

    public void setMonitor(RTBufferMonitor monitor) {
        this.monitor = monitor;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

}
