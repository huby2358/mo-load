package io.mo.tsbs;

import io.mo.util.TSBSConfUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DATA {
    public static int DEFAULT_BUFFER_SIZE = 1000;
    public static int DEFAULT_RESULT_BUFFER_SIZE = 1000;
    public static int DEFAULT_REPORT_INTERVAL = 10;
    public static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public static String EXECUTENAME = FORMAT.format(new Date());
    
    public static Date START_DATE;
    public static Date END_DATE;

    static {
        try {
            START_DATE = TSBSConfUtil.getStartTimestamp() == null?DATE_FORMAT.parse("1970-01-01"):DATE_FORMAT.parse(TSBSConfUtil.getStartTimestamp());
            END_DATE = TSBSConfUtil.getEndTimestamp() == null?DATE_FORMAT.parse("2030-13-31"):DATE_FORMAT.parse(TSBSConfUtil.getEndTimestamp());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    
    
}
