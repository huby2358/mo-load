package io.mo.replace;

import io.mo.CONFIG;

public class RandomBigintVariable implements Variable {
    private String name;
    private long start;


    private long end;
    private int scope = CONFIG.PARA_SCOPE_TRANSCATION;
    
    public void init(){};
    public String getName(){return this.name;}



    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }


    public RandomBigintVariable(String name, String range){
        this.name = name;

        String s = range.substring(0, range.indexOf(","));
        String e = range.substring(range.indexOf(",")+1,range.length());
        if(s != null){
            start = Long.parseLong(s);
        }
        if( e.equalsIgnoreCase("-")){
            end = 2000000000000L;
        }else{
            end = Long.parseLong(e);
        }
    }

    public synchronized String nextValue(){
        String value = String.valueOf((int) (Math.random() * (end - start + 1) + start));
        return value;
    }

    @Override
    public String getExpress() {
        return "{"+name+"}";
    }


    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
    
    public static void main(String[] args){
        RandomBigintVariable randomBigintVariable = new RandomBigintVariable("test","1,10000000000");
        System.out.println(randomBigintVariable.nextValue());
    }
}
