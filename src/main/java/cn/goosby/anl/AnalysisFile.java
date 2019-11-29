package cn.goosby.anl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Pacekage: cn.goosby.anl
 * @Author: goosby.liu
 * @Date: 2019/11/22 17:14
 * @Version: 14
 * @Description: quartz.test
 **/
public class AnalysisFile {

    private Logger logger = LoggerFactory.getLogger(AnalysisFile.class);

    public String fileName;

    public String url;

    public String corePoolSize;

    public String maximumPoolSize;

    public String keepAliveTime;

    public String capacity;

    public String filePath;

    private ThreadPoolExecutor threadPoolExecutor;


    public void initThreadPool() {
        String jvmCorePoolSize = System.getProperty("quartz.threadPool.corePoolSize");
        if (!StringUtils.hasLength(jvmCorePoolSize)) {
            jvmCorePoolSize = this.corePoolSize;
        }

        String jvmMaximumPoolSize = System.getProperty("quartz.threadPool.maximumPoolSize");
        if (!StringUtils.hasLength(jvmMaximumPoolSize)) {
            jvmMaximumPoolSize = this.maximumPoolSize;
        }
        String jvmKeepAliveTime = System.getProperty("quartz.threadPool.keepAliveTime");
        if (!StringUtils.hasLength(jvmKeepAliveTime)) {
            jvmKeepAliveTime = this.keepAliveTime;
        }
        String jvmCapacity = System.getProperty("quartz.threadPool.capacity");
        if (!StringUtils.hasLength(jvmCapacity)) {
            jvmCapacity = this.capacity;
        }


        if (threadPoolExecutor == null ) {
            threadPoolExecutor = new ThreadPoolExecutor(
                    Integer.valueOf(jvmCorePoolSize),
                    Integer.valueOf(jvmMaximumPoolSize),
                    Integer.valueOf(jvmKeepAliveTime),
                    TimeUnit.MINUTES,
                    new ArrayBlockingQueue<Runnable>(Integer.valueOf(jvmCapacity)));
        }
    }

    public void execute() {
        this.initThreadPool();
        String jvmCorePoolSize = System.getProperty("quartz.threadPool.corePoolSize");
        if (!StringUtils.hasLength(jvmCorePoolSize)) {
            jvmCorePoolSize = this.corePoolSize;
        }
        String finalJvmReadFile = System.getProperty("quartz.analysisFile.fileName");
        for (int i = 0; i < Integer.valueOf(jvmCorePoolSize); i++) {
            threadPoolExecutor.execute(() -> {
                this.callHostWithGet();
                final String fileBody = this.readFile(finalJvmReadFile);
                this.wirteFile(fileBody);
            });
        }
    }


    private String readFile(String file){
        StringBuffer stringBuffer = new StringBuffer();

        File xmlFile = null;
        if (StringUtils.hasLength(file)) {
            xmlFile = new File(file);
        } else {
            Resource resource = new ClassPathResource(fileName);
            try {
                xmlFile = resource.getFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedReader bufferedReader = null;
        FileReader fileReader = null;
        try {
            if (xmlFile != null) {

                fileReader = new FileReader(xmlFile);
                bufferedReader = new BufferedReader(fileReader);
                String str = null;
                while ((str = bufferedReader.readLine()) != null) {
                    stringBuffer.append(str);
                }
            }
        } catch (IOException e) {
            logger.error("read file exception", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return  stringBuffer.toString();
    }


    private void wirteFile(String xmlString) {
        String jvmFilePath = System.getProperty("quartz.write.file.path");
        if (!StringUtils.hasLength(jvmFilePath)) {
            jvmFilePath = this.filePath;
        }
        if (!jvmFilePath.endsWith("/")) {
            jvmFilePath = jvmFilePath + "/";
        }
        boolean result = false;
        String fileName = Thread.currentThread().getName() + "_" + System.currentTimeMillis() + ".xml";
        File writeFile = new File(jvmFilePath + fileName);
        if (!writeFile.exists()) {
            try {
                writeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fileWritter = null;
        BufferedWriter bufferWritter = null;
        try {
            fileWritter = new FileWriter(writeFile, true);
            bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(xmlString);
            result = true;
        } catch (IOException e) {
            logger.error("write file exception", e);
        } finally {
            try {
                bufferWritter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileWritter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("write file is: {} ,write result is: {} ,path is: {}", fileName, result, writeFile.getAbsolutePath());
    }

    public String callHostWithGet() {
        String jvmHostUrl = System.getProperty("quartz.get.host.url");
        if (!StringUtils.hasLength(jvmHostUrl)) {
            jvmHostUrl = this.url;
        }
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(jvmHostUrl);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            logger.info("call url is:{}, response status code is: {}", jvmHostUrl, response.getStatusLine().getStatusCode());
        } catch (IOException e) {
            logger.error("call host exception", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.getEntity().toString();
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCorePoolSize(String corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public void setMaximumPoolSize(String maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public void setKeepAliveTime(String keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}