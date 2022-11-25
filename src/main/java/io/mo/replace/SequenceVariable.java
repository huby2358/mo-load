package io.mo.replace;

public class SequenceVariable implements Variable {

    private String name;
    private long start;



    private int step = 1;

    public void init(){};
    public String getName(){return this.name;}

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
