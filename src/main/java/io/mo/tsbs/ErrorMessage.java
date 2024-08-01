package io.mo.tsbs;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ErrorMessage {
    
    private String errorTime = null;
    
    private String txnName = null;
    private String sql = null;
    private String sqlFileName = null;
    private String[] paras = null;
    private int errorCode = -1;
    private String errorMessage = null;

    private String requestTime = null;
    private StringBuffer buffer = new StringBuffer();

    private String expected = "false";
    
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public ErrorMessage(){
        this.errorTime = format.format(new Date());
    }
    
    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String[] getParas() {
        return paras;
    }

    public void setParas(String[] paras) {
        this.paras = paras;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public String getTxnName() {
        return txnName;
    }

    public void setTxnName(String txnName) {
        this.txnName = txnName;
    }


    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public String getSqlFileName() {
        return sqlFileName;
    }

    public void setSqlFileName(String sqlFileName) {
        this.sqlFileName = sqlFileName;
    }

    public String toString(){
        buffer.append("\n");
        
        buffer.append("ErrorTime : ");
        buffer.append(errorTime);
        buffer.append("\n");

        buffer.append("RequestTime : ");
        buffer.append(requestTime);
        buffer.append("\n");

        buffer.append("SQLFileName : ");
        buffer.append(sqlFileName);
        buffer.append("\n");


        buffer.append("ErrorCode : ");
        buffer.append(errorCode);
        buffer.append("\n");

        buffer.append("ErrorMessage : ");
        buffer.append(errorMessage);
        buffer.append("\n");
        
        return buffer.toString();
    }
    
}
