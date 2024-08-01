package io.mo.transaction.ts;

import io.mo.CONFIG;
import io.mo.replace.EnumVariable;
import io.mo.replace.RandomVariable;
import io.mo.replace.Variable;
import io.mo.transaction.SQLScript;
import io.mo.transaction.Transaction;
import io.mo.util.ReplaceConfigUtil;
import io.mo.util.RunConfigUtil;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

public class TSSQLScript extends SQLScript {
    
    private String org_devices = null;
    private String org_metrics = null;
    private int device_num = 1;
    private int batch_size = 1;
    private int buffer_size = 1;
    
    private String table_name = null;
    private String method = null;
    private int metric_count = 0;
    private String field_separator = "|";
    private String[] metric_types = null;
    
    private  ArrayList<Variable> device_vars = new ArrayList<>();
    private  ArrayList<String> devices = new ArrayList<>();
    
    private StringBuilder builder = null;
    private StringBuilder metric_values = null;
    
    private Random random = new Random();
    private Logger LOG = Logger.getLogger(SQLScript.class.getName());


    public static Calendar STARTTIME = Calendar.getInstance();
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     
    
    public TSSQLScript(String table_name, String org_devices,String org_metrics,int batch_size){
        this.table_name = table_name;
        this.org_devices = org_devices;
        this.org_metrics = org_metrics;
        this.batch_size = batch_size;
        init();
        int length = (int)((19 + org_devices.length() + org_devices.length())*batch_size*1.5);
        builder = new StringBuilder(length);
        
        metric_types = org_metrics.split(",");
        metric_count = metric_types.length;
        metric_values = new StringBuilder(metric_count*6);
    }
    
    public void init(){
        
        for(int i = 0; i < ReplaceConfigUtil.vars.size(); i++){
            if(org_devices.contains(ReplaceConfigUtil.vars.get(i).getExpress())) {
                device_vars.add(ReplaceConfigUtil.vars.get(i));
            }
            //System.out.println(ReplaceConfigUtil.vars.get(i).getExpress());
        }
        
        if(device_vars.size() == 0){
            devices.add(org_devices);
            return;
        }
        
        for(int i = 0; i < device_vars.size(); i++){
            Variable variable = device_vars.get(i);
            
            if(!(variable instanceof  RandomVariable) && 
                    !(variable instanceof EnumVariable)){
                LOG.error(String.format("Variable[%s] is not type of enum or random, please check!"));
                System.exit(1);
            }
            
            if(variable instanceof RandomVariable){
                device_num *= ((RandomVariable) variable).size();
            }
            
            if(variable instanceof EnumVariable){
                device_num *= ((EnumVariable) variable).size();
            }
        }

        LOG.info("device_num = " + device_num);
        
        initDeviceData(0,org_devices);
        
        buffer_size = gongbei(device_num,batch_size);
        LOG.info("buffer_size = " + buffer_size);
        
        super.setLength(buffer_size/batch_size);
    }
    
    public void initDeviceData(int i,String data){
        Variable variable = device_vars.get(i);

        if(!data.contains(variable.getExpress()))
            return;

        if(variable instanceof RandomVariable){
            int size = ((RandomVariable) variable).size();
            for(int j = 0; j < ((RandomVariable) variable).size();j++) {
                String semi_data = data.replace(variable.getExpress(), ((RandomVariable) variable).getValue(j));
                //System.out.println(String.format("name = %s, value = %s",variable.getName(),((RandomVariable) variable).getValue(j)));
                if(i < device_vars.size() - 1){
                    initDeviceData(i+1,semi_data);
                }
                
                if(i == device_vars.size() -1)
                    devices.add(semi_data);
            }
        }

        if(variable instanceof EnumVariable){
            int size = ((EnumVariable) variable).size();
            for(int j = 0; j < ((EnumVariable) variable).size();j++) {
                String semi_data = data.replace(variable.getExpress(), ((EnumVariable) variable).getValue(j));
                //System.out.println(String.format("name = %s, value = %s",variable.getName(),((EnumVariable) variable).getValue(j)));

                if(i < device_vars.size() - 1){
                    initDeviceData(i+1,semi_data);
                }
                if(i == device_vars.size() -1)
                    devices.add(semi_data);
            }
        }
        
    }

    public SQLScript createNewScript(){
        int count = 1;
        SQLScript script = new SQLScript(buffer_size/batch_size);
        script.setBatchSize(batch_size);
        builder.setLength(0);
        if(method.equalsIgnoreCase("insert")) {
            builder.append("INSERT INTO " + table_name + " VALUES('");
            for (int i = 0; i < buffer_size; ) {
                String timestampe = getDateTime();
                for (int j = 0; j < device_num; j++) {

                    //add timestamp
                    builder.append(timestampe);
                    builder.append(".");
                    builder.append(random.nextInt(1000));
                    builder.append("',");

                    //add device data
                    builder.append(devices.get(j));
                    builder.append(",");

                    //add metrix data
                    builder.append(newMectircs());
                    builder.append(")");

                    if (count != batch_size) {
                        builder.append(",('");
                        count++;
                    } else {
                        //System.out.println(builder.toString());
                        script.addCommand(builder.toString());
                        builder.setLength(0);
                        builder.append("INSERT INTO " + table_name + " VALUES('");
                        count = 1;
                    }
                }
                i += device_num;
            }
        }

        if(method.equalsIgnoreCase("load")) {
            builder.append("LOAD DATA INLINE FORMAT='csv', DATA='");
            String tail  = " INTO TABLE " + table_name +" fields terminated by '"+field_separator+"' lines terminated by '\\r\\n'";
            for (int i = 0; i < buffer_size; ) {
                String timestampe = getDateTime();
                for (int j = 0; j < device_num; j++) {

                    //add timestamp
                    builder.append(timestampe);
                    builder.append(".");
                    builder.append(random.nextInt(1000));
                    builder.append(field_separator);

                    //add device data
                    builder.append(devices.get(j));
                    builder.append(field_separator);

                    //add metrix data
                    builder.append(newMectircs());

                    if (count != batch_size) {
                        builder.append("\r\n");
                        count++;
                    } else {
                        //System.out.println(builder.toString());
                        builder.append("' ");
                        builder.append(tail);
                        script.addCommand(builder.toString());
                        builder.setLength(0);
                        builder.append("LOAD DATA INLINE FORMAT='csv', DATA='");
                        count = 1;
                    }
                }
                i += device_num;
            }
        }
        
        
        return script;
    }

    public int findLeastCommonMultiple(int a, int b) {
        return a * b / gcd(a, b);
    }

    public int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public int gongyue(int min, int max) {
        while(max%min!=0){
            int temp=max%min;
            max=min;
            min=temp;
        }
        return min;
    }

    public int gongbei(int min, int max) {
        if(min > max){
            min = min + max;
            max = min - max;
            min = min - max;
        }
        
        long prod = (long)min * (long)max;
        long res = prod / gongyue(min, max);
        if(res > Integer.MAX_VALUE){
            LOG.error("Device count is too large!");
            System.exit(1);
        }
            
        return (int)(prod / gongyue(min, max));
    }
    
    public String newMectircs(){
        metric_values.setLength(0);
        
        for(int i = 0; i < metric_count; i++){
            if(metric_types[i].equalsIgnoreCase("int"))
                metric_values.append(random.nextInt(100));

            if(metric_types[i].equalsIgnoreCase("float")) {
                metric_values.append(random.nextInt(100));
                metric_values.append(".");
                metric_values.append(random.nextInt(100));
            }
            
            if(i < metric_count -1 ) {
                if(method.equalsIgnoreCase("insert"))
                    metric_values.append(",");
                    

                if(method.equalsIgnoreCase("load"))
                    metric_values.append(field_separator);
                        
            }
        }
        
        return metric_values.toString();
    }

    public int getBatchSize() {
        return batch_size;
    }

    public void setBatchSize(int batch_size) {
        this.batch_size = batch_size;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getOrg_devices() {
        return org_devices;
    }

    public void setOrg_devices(String org_devices) {
        this.org_devices = org_devices;
    }

    public String getOrg_metrics() {
        return org_metrics;
    }

    public void setOrg_metrics(String org_metrics) {
        this.org_metrics = org_metrics;
    }

    public String getField_separator() {
        return field_separator;
    }

    public void setField_separator(String field_separator) {
        this.field_separator = field_separator;
        if(method.equalsIgnoreCase("load")) {
            if(this.field_separator.equalsIgnoreCase("|"))
                metric_types = org_metrics.split("\\|");
            else
                metric_types = org_metrics.split(this.field_separator);
            metric_count = metric_types.length;
        }
    }

    public int getBuffer_size() {
        return buffer_size;
    }

    public void setBuffer_size(int buffer_size) {
        this.buffer_size = buffer_size;
    }

    public synchronized static String getDateTime(){
        STARTTIME.add(Calendar.SECOND,1);
        return format.format(STARTTIME.getTime());
    }
    
    public static void main(String[] args){
        Transaction transaction = RunConfigUtil.getTransaction(1);
        TSSQLScript script = (TSSQLScript)transaction.getScript();
        System.out.println("script.file_seperator = " + script.getField_separator());
        SQLScript script1 = script.createNewScript();

        for(int i = 0; i < script1.length(); i++) {
            System.out.println(script1.getCommand(i));
        }

        SQLScript script2 = script.createNewScript();
        for(int i = 0; i < script1.length(); i++) {
            System.out.println(script2.getCommand(i));
        }
        TSSQLScript tssqlScript = new TSSQLScript("null","null","null",20000);
        System.out.println(tssqlScript.gongbei(900000,20000));
        //System.exit(1);
    }
}
