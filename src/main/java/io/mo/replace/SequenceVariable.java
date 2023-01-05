package io.mo.replace;

import io.mo.CONFIG;

public class SequenceVariable implements Variable {

    private String name;
    private long start;
    private int step = 1;

    private int scope = CONFIG.PARA_SCOPE_TRANSCATION;

    public void init(){};
    public String getName(){return this.name;}
    
    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public SequenceVariable(String name, long start){
        this.name = name;
        this.start = start;
    }

    public synchronized String nextValue(){
        long value = this.start;
        start += step;
        return String.valueOf(value);
    }

    @Override
    public String getExpress() {
        return "{"+name+"}";
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

}
