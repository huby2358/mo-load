package io.mo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CONFIG {


    public static int DEFAULT_SIZE_PER_VAR = 100000000;//默认每个文件变量的最大使用容量

    public static int DEFAULT_SIZE_SEND_BUFFER_PER_THREAD = 1000;//每个执行线程的发送缓冲区的大小

    public static Boolean TIMEOUT = false;//是否已经执行结束

    public static int TEMP_RT_BUF_SIZE_PER_THREAD = 100000;
    public static int TEMP_ERROR_BUF_SIZE = 100000;

    public static int RT_BUFFER_FLUSH_SIZE = 500000;

    public static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public static String EXECUTENAME = FORMAT.format(new Date());

    public static int DB_TRANSACTION_MODE = 1;
    public static int NOT_DB_TRANSACTION_MODE = 0;



}
