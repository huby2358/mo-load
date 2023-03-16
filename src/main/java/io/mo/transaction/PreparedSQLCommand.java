package io.mo.transaction;

import io.mo.CONFIG;
import io.mo.MOPerfTest;
import io.mo.para.PreparedPara;
import io.mo.replace.RandomVariable;
import io.mo.replace.SequenceVariable;
import io.mo.replace.Variable;
import io.mo.thread.PreparedParaProducer;
import io.mo.util.ReplaceConfigUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedSQLCommand {
    private String org_sql = null;
    private String[] ps_sql;
    private PreparedStatement[] ps;

    private RandomVariable randomVariable;

    private Connection connection;
    
    private PreparedPara[] preparedParas;
    
    private int run_pos = 0;

    private static Logger LOG = Logger.getLogger(PreparedSQLCommand.class.getName());

    public String getOriginalSql() {
        return org_sql;
    }

    public void setOriginalSql(String sql) {
        this.org_sql = org_sql;
    }


    public PreparedSQLCommand(String org_sql){
        this.org_sql = org_sql;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    
    public boolean prepare(){
        if(containSequenceVars()){
            return false;
        }
        
        if(containRandomVars()){
            int start = randomVariable.getStart();
            int end = randomVariable.getEnd();
            ps_sql = new String[end - start + 1];
            ps = new PreparedStatement[end - start + 1];
            for(int i = 0; i < ps_sql.length; i++){
                ps_sql[i] = org_sql.replaceAll("\\{" + randomVariable.getName() + "\\}", String.valueOf(i + start));
                try {
                    ps[i] = connection.prepareStatement(ps_sql[i]);
                    LOG.debug(String.format("Succeed to prepare statemet [%s].", ps_sql[i]));
                } catch (SQLException e) {
                    LOG.error(String.format("Failed to prepare statement [%s].", ps_sql[i]));
                    return false;
                }
            }
            return true;
        }

        ps = new PreparedStatement[1];
        try {
            ps[0] = connection.prepareStatement(org_sql);
        } catch (SQLException e) {
            LOG.error(String.format("Failed to prepare statement [%s].", ps_sql[0]));
            return false;
        }
        
        return true;
    }
    
    public void execute() throws SQLException {
        for (int j = 0; j < preparedParas.length; j++) {
            if (preparedParas[j].isINT()) {
                ps[run_pos].setInt(j + 1, preparedParas[j].getIntValue());
            }
            if (preparedParas[j].isSTR()) {
                ps[run_pos].setString(j + 1, preparedParas[j].getStrValue());
            }
            ps[run_pos].execute();
        }
        run_pos ++;
        if(run_pos == ps.length -1)
            run_pos = 0;
    }
    
    public boolean containRandomVars(){
        for(int i = 0;i < ReplaceConfigUtil.vars.size();i++) {
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            if(org_sql.indexOf("{"+var.getName()+"}") != -1 && var instanceof RandomVariable) {
                randomVariable = (RandomVariable) var;
                return true;
            }
        }
        return false;
    }

    public boolean containSequenceVars(){
        for(int i = 0;i < ReplaceConfigUtil.vars.size();i++) {
            Variable var = (Variable) ReplaceConfigUtil.vars.get(i);
            if(org_sql.indexOf("{"+var.getName()+"}") != -1 && var instanceof SequenceVariable) {
                LOG.error("The sql command in prepare mode can not contain variable type of sequence.");
                LOG.error("sql command : " + org_sql);
                LOG.error("variable : " + var.getName());
                return true;
            }
        }
        return false;
    }
    
    public PreparedPara[] getPreparedParas() {
        return preparedParas;
    }

    public void setPreparedParas(PreparedPara[] preparedParas) {
        this.preparedParas = preparedParas;
    }


    public void parseParas(String paras){
        if(paras == null){
            preparedParas = new PreparedPara[0];
            return;
        }
        
        String[] array = paras.split(",");
        preparedParas = new PreparedPara[array.length];
        for(int i = 0;i < array.length;i++){
            if(array[i].startsWith("INT")){
                PreparedPara para = new PreparedPara("INT",array[i].substring(4,array[i].length()-1));

                preparedParas[i] = para;
            }

            if(array[i].startsWith("STR")){
                PreparedPara para = new PreparedPara("STR",array[i].substring(4,array[i].length()-1));
                preparedParas[i] = para;
            }
        }
    }
}
