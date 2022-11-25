package io.mo.replace;


import io.mo.CONFIG;
import io.mo.MOPerfTest;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CountDownLatch;

public class FileVariable implements Variable {

    static final long MAX_MAPPING_SIZE = 1024*1024*1024;

    private String name;
    private String path;
    private String[] values;
    private  int pos = 0;
    private static Logger LOG = Logger.getLogger(MOPerfTest.class.getName());

    public FileVariable(String name, String path){
        this.name = name;
        this.path = path;
    }

    public String getName(){return this.name;}

    @Override
    public String getExpress() {
        return "{"+name+"}";
    }

    @Override
    public String nextValue(){
        int index = (int) (Math.random() * (values.length - 1));
        return values[index];
    }

    public synchronized void setValue(String value){

        if(pos >= values.length){
            return;
        }

        values[pos++] = value;
    }

    public void init(){
        long start = - System.currentTimeMillis();
        final File file = new File(path);
        long length = file.length();

        if(length <= 50*1024*1024){
            /*
             *小文件加载流程
             */
            initForSmallFile(file);
        }else {
            /*
             *大文件加载流程
             */
            initForLargeFile(file);
        }

        LOG.info(	"time = " + (System.currentTimeMillis() + start)+" pos = "+pos);

    }


    /*
     *读取变量文件到变量数组
     */

    public void read(String path){
        try{
            FileReader fileReader = new FileReader(path);

            LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
            lineNumberReader.skip(Long.MAX_VALUE);
            int lines = lineNumberReader.getLineNumber();

            if(lines > CONFIG.DEFAULT_SIZE_PER_VAR) lines = CONFIG.DEFAULT_SIZE_PER_VAR;
            values = new String[lines];
            LOG.info("The Program will init "+lines+" values for variable["+name+"]");

            RandomAccessFile accessFile = new RandomAccessFile(path, "r");
            File file = new File(path);
            long length = file.length();

            for(int i = 0; i < values.length; i++){
                long pos = (long)Math.random()*length;
                accessFile.seek(pos);
                String line = accessFile.readLine();
                if(line == null){
                    i--;
                    continue;
                }
                values[i] = line;
                if(i%100000  == 0){
                    LOG.info("The "+i+" variables["+name+"] have been initialiazed");
                }

            }

            System.out.println("values length = "+values.length);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     *对于小于300兆的文件，直接采用NIO的方式读取
     */
    public void initForSmallFile(File file){
        try  {
            FileReader fileReader = new FileReader(file);
            LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
            lineNumberReader.skip(Long.MAX_VALUE);
            int lines = lineNumberReader.getLineNumber();

            if(lines > CONFIG.DEFAULT_SIZE_PER_VAR) lines = CONFIG.DEFAULT_SIZE_PER_VAR;
            values = new String[lines];
            LOG.info("The Program will init "+lines+" values for variable["+name+"]");

            fileReader.close();

            fileReader = new FileReader(path);

            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String input = null;
            int index = 0;

            while((input = bufferedReader.readLine()) != null){
                setValue(input);
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     *对于大于300M的文件，采用内存映射的方式读取
     *会根据文件大小分成不同线程处理，最多处理CONSTANTS.DEFAULT_SIZE_PER_VAR个记录
     *为保证加载的每条记录都和文件中的一整行对应，会在加载内存时，丢掉部分记录，可能存在文件中的少数几个记录永远不会被使用
     */
    public void initForLargeFile(File file){
        try{
            LOG.info("Calculate the size of the variable file : "+file.getPath());
            FileReader fileReader = new FileReader(file);
            LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
            lineNumberReader.skip(Long.MAX_VALUE);

            int lines = lineNumberReader.getLineNumber();
            if(lines > CONFIG.DEFAULT_SIZE_PER_VAR)
                lines = CONFIG.DEFAULT_SIZE_PER_VAR;

            LOG.info("The Program will init "+lines+" values for variable["+name+"]");

            fileReader.close();

            long start = - System.currentTimeMillis();
            //final File file = new File(path);
            long length = file.length();
            int defaultThreadNum = 4;//Runtime.getRuntime().availableProcessors();

            /**
             * 计算每个映射空间的大小
             */
            long size = length/defaultThreadNum;

            /*
             *计算总共需要多少个映射，每个映射由独立线程操作
             */
            defaultThreadNum = (int)((length + size -1)/size);

            /*
             *如果size大于定义的MAX_MAPPING_SIZE，则除最后一个映射外，其他映射的size均为MAX_MAPPING_SIZE
             */
            if (size > MAX_MAPPING_SIZE) {
                defaultThreadNum = (int) ((length + MAX_MAPPING_SIZE -1) / MAX_MAPPING_SIZE) ;
                //size = length/defaultThreadNum;
                size = MAX_MAPPING_SIZE;
            }

            /*
             *考虑到每块映射都有可能丢弃第一行，和最后一行，所以，数组总长度-2*映射线程数
             */
            values = new String[lines - defaultThreadNum*2];

            final CountDownLatch cdl = new CountDownLatch(defaultThreadNum+1);
            long offset = 0;
            long remain = length - offset;



            for (int threadNum = 0;threadNum < defaultThreadNum;threadNum++) {

                if (size > remain) {
                    size = remain;
                }

                try {
                    final long offsetF = offset;
                    final long sizeF = size;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            MappedByteBuffer mappedByteBuffer = null;
                            try {
                                LOG.info("freeMemory = " + Runtime.getRuntime().freeMemory() + " offsetF=" + offsetF + " sizeF=" + sizeF);
                                mappedByteBuffer = getMappedByteBuffer(file, MapMode.READ_ONLY, offsetF, sizeF);
                                while (null == mappedByteBuffer) {
                                    try {
                                        Thread.sleep(1000);
                                        mappedByteBuffer = getMappedByteBuffer(file, MapMode.READ_ONLY, offsetF, sizeF);
                                    }
                                    catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        //e.printStackTrace();
                                    }
                                }
                                LOG.info("mappedByteBuffer =" + mappedByteBuffer);
                                //boolean isChangeLineAtLast = atLastIsChangeLine(mappedByteBuffer);
                                //LOG.info("isChangeLineAtLast = "+isChangeLineAtLast);
                                String content = byteBufferToString(mappedByteBuffer);
                                LineNumberReader read = new  LineNumberReader(new StringReader(content));
                                /*
                                 *去掉第一行,以防止第一行不是整行
                                 */
                                read.readLine();

                                String line = read.readLine();
                                while (null != line) {
                                    String next = read.readLine();
                                    if(null == next){
                                        /*
                                         *说明已经读到最后一行,去掉
                                         * 去掉最后一行，以防止最后一行不是整行
                                         */
                                        break;
                                    }

                                    setValue(line);
                                    line = next;
                                    /*
                                     *如果达到最大容量，结束
                                     */
                                    if(pos >= values.length){
                                        cdl.countDown();
                                        break;
                                    }
                                }

                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            finally {
                                if (null != cdl){
                                    cdl.countDown();
                                }
                                unmap(mappedByteBuffer);
                                //LOG.info(Thread.currentThread().getName()+"has ended.");
                            }


                        }
                    }).start();

                    offset += size;
                    remain = length - offset;
                }
                catch (Exception e) {
                    cdl.countDown();
                    e.printStackTrace();
                }
            }



            //启动线程监控文件加载进度
            new Thread(new Runnable()
            {

                @Override
                public void run()
                {
                    while(true){
                        if(pos < values.length)
                            LOG.info("The "+pos+" variables["+name+"] have been initialiazed");
                        else{
                            cdl.countDown();
                            break;
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();



            //等待加载结束
            cdl.await();

        }catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     *将bytebuffer转换成字符串
     */
    public static String byteBufferToString(ByteBuffer buffer) {
        CharBuffer charBuffer = null;
        try {
            Charset charset = Charset.defaultCharset();
            CharsetDecoder decoder = charset.newDecoder();
            //charBuffer = decoder.decode(buffer);
            charBuffer = charset.decode(buffer);
            buffer.flip();
            return charBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /***
     * 该映射区 最后一个字节 是否是换行符
     */
    public static boolean atLastIsChangeLine(MappedByteBuffer mappedByteBuffer)
    {
        if (null == mappedByteBuffer)
        {
            return false;
        }
        int bufferLength = mappedByteBuffer.limit();
        byte indexByte = mappedByteBuffer.get(bufferLength - 1);
        if ('\n' == indexByte)
        {
            return true;
        }
        return false;
    }


    /***
     * 映射文件
     * @param file 文件
     * @param fileMappedMode 映射模式
     * @param offset 起始位置
     * @param size 映射缓存大小
     * @return
     */
    public static MappedByteBuffer getMappedByteBuffer(File file,MapMode fileMappedMode,long offset,long size)
    {
        if (null == file)
        {
            LOG.error("file is null! ");
            return null;
        }

        /*if (size > MAX_MAPPING_SIZE)
        {
            LOG.error("Mapped FileVariable length is to Big! ");
            return null;
        }*/


        if (offset < 0)
        {
            LOG.error("offset is less then 0");
            return null;
        }

        if (null == fileMappedMode)
        {
            LOG.error(" fileMappedMode  couldn't be null !");
            return null;
        }

        String randomMode = "r";
        if (fileMappedMode == MapMode.READ_WRITE|| fileMappedMode == MapMode.PRIVATE)
        {
            randomMode = "rw";
        }
        try
        {
            MappedByteBuffer map = new RandomAccessFile(file,randomMode).
                    getChannel().map(fileMappedMode, offset, size);
            return map;
        }
        catch (Exception e)
        {
            // e.printStackTrace();
            LOG.error("getMappedByteBuffer catch Exception = [" + e.getMessage() + "]",e);
        }
        return null;
    }

    public static void unmap(final MappedByteBuffer mappedByteBuffer) {
        if (mappedByteBuffer == null)
        {
            return;
        }
        AccessController.doPrivileged(new PrivilegedAction<Object>()
        {
            public Object run() {
                try {
                    Method getCleanerMethod = mappedByteBuffer.getClass().getMethod("cleaner", new Class[0]);
                    if (getCleanerMethod != null) {
                        getCleanerMethod.setAccessible(true);
                        Object cleaner = getCleanerMethod.invoke(mappedByteBuffer, new Object[0]);
                        Method cleanMethod = cleaner.getClass().getMethod("clean", new Class[0]);
                        if (cleanMethod != null) {
                            cleanMethod.invoke(cleaner, new Object[0]);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        });
    }


    public static void main(String args[]){
        //FileVariable file = new FileVariable("tyxym_prefix",args[0]);
        FileVariable file = new FileVariable("zhongwen","replace/zhongwen.txt");
        //file.init();
        file.initForLargeFile(new File("replace/zhongwen.txt"));
        /*for(int i = 0; i < file.values.length;i++){
            System.out.println(file.values[i]);
        }*/
    }


}

