package io.mo.transaction.ts;

import io.mo.para.PreparedPara;
import io.mo.thread.PreparedParaProducer;
import io.mo.transaction.SQLScript;
import io.mo.transaction.Transaction;

import java.util.ArrayList;

public class TSTransaction extends Transaction {
    
    
    public TSTransaction(String name,int theadnum){
        super(name,theadnum);
    }
    
    

    public static void main(String[] args){
        int i = 0;
        System.out.println("i = "+(++i));
        System.out.println("i = "+(i++));
    }

}
