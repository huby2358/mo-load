package io.mo.thread;

import io.mo.para.PreparedPara;
import io.mo.transaction.PreparedSQLCommand;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PrepareStatmentExecutor {
    private PreparedSQLCommand command;

    private String[] ps_sql;
    private PreparedStatement[] ps;
    private PreparedPara[] preparedParas;

    private Connection connection;

    private int run_pos = 0;

    private static Logger LOG = Logger.getLogger(PrepareStatmentExecutor.class.getName());

    public PrepareStatmentExecutor(Connection connection, PreparedSQLCommand command, PreparedPara[] preparedParas){
        this.command = command;
        this.connection = connection;
        this.preparedParas = preparedParas;
    }

    public boolean prepare(){
        if(command.containSequenceVars()){
            return false;
        }

        if(command.containRandomVars()){
            int start = command.getRandomVariable().getStart();
            int end = command.getRandomVariable().getEnd();
            ps_sql = new String[end - start + 1];
            ps = new PreparedStatement[end - start + 1];
            for(int i = 0; i < ps_sql.length; i++){
                ps_sql[i] = command.getOriginalSql().replaceAll("\\{" + command.getRandomVariable().getName() + "\\}", String.valueOf(i + start));
                try {
                    ps[i] = connection.prepareStatement(ps_sql[i]);
                    LOG.debug(String.format("Succeed to prepare statemet [%s].", ps_sql[i]));
                } catch (SQLException e) {
                    LOG.error(String.format("Failed to prepare statement [%s].", ps_sql[i]));
                    LOG.error(e.getMessage());
                    return false;
                }
            }
            return true;
        }

        ps = new PreparedStatement[1];
        try {
            ps[0] = connection.prepareStatement(command.getOriginalSql());
            LOG.debug(String.format("Succeed to prepare statemet [%s].", command.getOriginalSql()));
        } catch (SQLException e) {
            LOG.error(String.format("Failed to prepare statement [%s].", ps_sql[0]));
            LOG.error(e.getMessage());
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

        if(run_pos == ps.length -1)
            run_pos = 0;
        else
            run_pos ++;
    }



    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}
