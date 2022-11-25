package io.mo.transaction;

public class SQLScript {
    private int pos = 0;
    private String[] commands;

    public SQLScript(int length){
        commands = new String[length];
    }

    public String getCommand(int i){
        return commands[i];
    }

    public void addCommand(String command){
        commands[pos++] = command;
    }

    public void replaceAll(String orc,String des){
        for(int i = 0; i < commands.length; i++){
            commands[i] = commands[i].replaceAll(orc,des);
        }
    }

    public int indexOf(String str){
        for(int i = 0; i < commands.length; i++){
            if(commands[i].indexOf(str) != -1)
                return commands[i].indexOf(str);
        }
        return -1;
    }

    public int length(){
        return commands.length;
    }

    public static void main(String[] args){
        String sql = "select JNSJ,TYXYM,GJ_ID,JNND,ZJLX,ZJHM,JNLX,JNDWMC,YW_UPDATE_TIME,JHPT_UPDATE_FLAG,JNFL,JHPT_UPDATE_TIME,JFJS,TASKID,JHPT_DELETE,XM,JNJE  from dwd_zwy_zgjfqk_iwi where zjhm$datetime ='{zjhm}';";
        SQLScript script = new SQLScript(1);
        script.addCommand(sql);
        script.replaceAll("\\{zjhm\\}","3143124321");
        System.out.println(script.getCommand(0));
    }
}
