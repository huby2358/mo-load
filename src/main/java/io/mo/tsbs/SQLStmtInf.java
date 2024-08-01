package io.mo.tsbs;

public interface SQLStmtInf {
    int metricCount();
    int recordCount();
    SQLStmtInf newInstance();
    String getSQL();
}
