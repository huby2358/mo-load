package io.mo.replace;

import io.mo.CONFIG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class EnumVariable implements Variable {
    private String name;
    private ArrayList<String> values = new ArrayList<>();
    private int size = 0;
    private Random random = new Random();
    private int scope = CONFIG.PARA_SCOPE_TRANSCATION;
    
    public void init(){};
    public String getName(){return this.name;}



    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public EnumVariable(String name, String org_values){
        this.name = name;
        String[] array_values = org_values.split(",");
        for(int i = 0; i < array_values.length;i++)
            values.add(array_values[i]);
        
        size = values.size();
    }

    public String nextValue(){
        
        return values.get(random.nextInt(size));
    }

    @Override
    public String getExpress() {
        return "{"+name+"}";
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
    public String getValue(int i){
        return values.get(i);
    }

    public static void main(String[] args){
        EnumVariable enumVariable = new EnumVariable("test","1,2,a,b,c");
        int[] count = {0,0,0,0,0};
        for(int i = 0; i < 10000; i++) {
            switch (enumVariable.nextValue()){
                case "1":
                    count[0]++;
                    break;
                case "2":
                    count[1]++;
                    break;
                case "a":
                    count[2]++;
                    break;
                case "b":
                    count[3]++;
                    break;
                case "c":
                    count[4]++;
                    break;
                default:
                    break;
            }
        }
        
        System.out.println(Arrays.toString(count));
    }
}
