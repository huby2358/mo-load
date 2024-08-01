package io.mo.tsbs.cpuonly;

import io.mo.CONFIG;
import io.mo.conn.ConnectionOperation;
import io.mo.util.TSBSConfUtil;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class Benchmark {
    public static String db_name = TSBSConfUtil.getTsdsDb();
    
    public static int batch_size = TSBSConfUtil.getTsdsBatchSize();
    
    public static int loadWorkerNum = TSBSConfUtil.getLoaderWoker();
    
    public static String ddl_file = TSBSConfUtil.getDDFFile();
    
    public static Random random = new Random();

    private static Logger LOG = Logger.getLogger(Benchmark.class.getName());
    
    public Benchmark(){
        
    }
    
    public static void main(String[] args){

        Options options = new Options();
        options.addOption("h",true,"The server or proxy address");
        options.addOption("P",true,"The server or proxy port");
        options.addOption("u",true,"The username of conneciton to server");
        options.addOption("p",true,"The password of connection user to server");
        options.addOption("t",true,"The thread number per transaction");
        options.addOption("d",true,"The duration that all transactions will run");
        options.addOption("b","db",true,"The duration that all transactions will run");
        options.addOption("c",true,"The DDL file path ");
        
        CommandLineParser parser = new DefaultParser();
        try {

            CommandLine cmd = parser.parse(options,args);

            if(cmd.hasOption("h")) {
                CONFIG.SPEC_SERVER_ADDR = cmd.getOptionValue('h');
                LOG.info("server addr = " + cmd.getOptionValue('h'));
            }

            if(cmd.hasOption("P")) {
                CONFIG.SPEC_SERVER_PORT = Integer.parseInt(cmd.getOptionValue('P'));
                LOG.info("server port = " + cmd.getOptionValue('P'));
            }

            if(cmd.hasOption("u")) {
                CONFIG.SPEC_USERNAME = cmd.getOptionValue('u');
                LOG.info("username = " + cmd.getOptionValue('u'));
            }

            if(cmd.hasOption("p")) {
                CONFIG.SPEC_PASSWORD = cmd.getOptionValue('p');
                LOG.info("password = " + cmd.getOptionValue('p'));
            }

            if(cmd.hasOption("b")) {
                CONFIG.SPEC_DATABASE = cmd.getOptionValue("b");
                LOG.info("database = " + cmd.getOptionValue("b"));
            }

            if(cmd.hasOption("c")) {
                ddl_file = cmd.getOptionValue("c");
                LOG.info("ddlfile = " + cmd.getOptionValue("b"));
            }

            if(cmd.hasOption("t"))
                loadWorkerNum = Integer.parseInt(cmd.getOptionValue('t'));

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        
        initDB();

        initDir();
        
        
        
        //ExecutorService executor = Executors.newFixedThreadPool(loadWorkerNum);
        
    }
    
    public static void initDB(){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(ddl_file));
            StringBuilder ddl = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null){
                ddl.append(line + "\n");
            }
            
            Connection connection = ConnectionOperation.getConnection();
            
            Statement stmt = connection.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + db_name +";");
            stmt.execute("USE " + db_name +";");
            System.out.println(ddl.toString());
            stmt.execute(ddl.toString());
            LOG.info("Successfully initialized ");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void initTagData(){
        
    }
    
    public static void initDir(){
        File error_dir = new File("report/" + CONFIG.EXECUTENAME + "/error/");

        if(!error_dir.exists())
            error_dir.mkdirs();
    }
    
    
}
