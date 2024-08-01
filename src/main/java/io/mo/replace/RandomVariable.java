package io.mo.replace;

import io.mo.CONFIG;

public class RandomVariable implements Variable {
    private String name;
    private int start;


    private int end;
    private int scope = CONFIG.PARA_SCOPE_TRANSCATION;
    
    public void init(){};
    public String getName(){return this.name;}



    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }


    public RandomVariable(String name, String range){
        this.name = name;

        String s = range.substring(0, range.indexOf(","));
        String e = range.substring(range.indexOf(",")+1,range.length());
        if(s != null){
            start = Integer.parseInt(s);
        }
        if( e.equalsIgnoreCase("-")){
            end = 2000000000;
        }else{
            end = Integer.parseInt(e);
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


    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
    
    public String getValue(int i){
        return String.valueOf(start + i);
    }
    
    public int size(){
        return end - start + 1;
    }
}
