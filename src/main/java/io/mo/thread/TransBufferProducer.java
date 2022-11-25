package io.mo.thread;

import io.mo.CONFIG;
import io.mo.transaction.TransBuffer;
import io.mo.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransBufferProducer extends Thread {


    private List<TransBuffer> buffers = new ArrayList<>();
    private Transaction transaction;

    private int write_total = 0;
    private int read_total = 0;

    private boolean terminated = false;

    public TransBufferProducer(){

    }

    public void addBuffer(TransBuffer buffer){
        buffers.add(buffer);
    }

    @Override
    public void run(){
        while(!terminated){
            for (int i = 0 ; i < buffers.size(); i++){
                //buffers.get(i).getScript();
                buffers.get(i).forward();

            }
        }

    }


    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public List<TransBuffer> getBuffers() {
        return buffers;
    }

    public void setBuffers(List<TransBuffer> buffers) {
        this.buffers = buffers;
    }

    public int getWrite_total(){
        for (int i = 0 ; i < buffers.size(); i++){
            write_total += buffers.get(i).getWrite_time()- CONFIG.DEFAULT_SIZE_SEND_BUFFER_PER_THREAD;
        }
        return write_total;
    }

    public int getRead_total(){
        for (int i = 0 ; i < buffers.size(); i++){
            read_total += buffers.get(i).getRead_time();
        }
        return read_total;
    }

}
