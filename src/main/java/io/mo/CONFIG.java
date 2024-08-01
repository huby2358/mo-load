package io.mo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CONFIG {


    public static int DEFAULT_SIZE_PER_VAR = 100000000;//默认每个文件变量的最大使用容量

    public static int DEFAULT_SIZE_SEND_BUFFER_PER_THREAD = 2000;//每个执行线程的发送缓冲区的大小

    public static int DEFAULT_SIZE_PREPARED_PARA_PER_THREAD = 10000;//每个执行线程的发送缓冲区的大小

    public static Boolean TIMEOUT = false;//是否已经执行结束

    public static int TEMP_RT_BUF_SIZE_PER_THREAD = 100000;
    public static int TEMP_ERROR_BUF_SIZE = 100000;

    public static int RT_BUFFER_FLUSH_SIZE = 500000;

    public static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public static String EXECUTENAME = FORMAT.format(new Date());

    public static int DB_TRANSACTION_MODE = 1;
    public static int NOT_DB_TRANSACTION_MODE = 0;
    
    public static int PARA_SCOPE_TRANSCATION = 0;//变量的作用域为事务
    public static int PARA_SCOPE_STATEMENT = 1;//变量的作用域为SQL

    public static String SPEC_SERVER_ADDR = null;
    public static int SPEC_SERVER_PORT = 6001;
    public static String SPEC_USERNAME = null;
    public static String SPEC_PASSWORD = null;
    public static String SPEC_DATABASE = null;
    
    public static boolean SHUTDOWN_SYSTEMOUT = false;
    
    public static int REPORT_INTERVAL = 10000;
    
    public static boolean SHORT_CONN_MODE = false;
    
    public static int TS_DEFAULT_BATCH_SIZE = 10000;
    public static String TS_DEFAULT_LOAD_METHOD = "load";
    public static String TS_DEFAULT_TABLE_NAME = "device";
    
    public static final String TXN_TYPE_COMMON = "common";
    public static final String TXN_TYPE_TS = "ts";
    
}
