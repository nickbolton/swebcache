package net.unicon.webcache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class CacheUtility implements InitializingBean {
    
    private HttpClient httpClient;
    private CacheWrapper cacheWrapper;
    private String persistentFileCachePath;
    private String fileSeparator;
    private Log log = LogFactory.getLog(getClass());
    
    public CacheUtility() {
        httpClient = new HttpClient();
        fileSeparator = System.getProperty("file.separator");
    }
    
    protected void storePreviousVersion(String url, String content) {
        PrintWriter writer = null;
        try {
            File filePath = getFilePath(url);
            
            if (log.isDebugEnabled()) {
                log.debug("Storing previous version for url="+url+" to '"+filePath.getAbsolutePath()+"'.");
            }
            
            writer = new PrintWriter(new FileWriter(filePath));
            writer.println(content);
        } catch (IOException e) {
            log.error("Failed storing the previous version of url: " + url, e);
        } finally {
            writer.close();
        }
    }
    
    protected File getFilePath(String urlStr) {
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            log.error("Malformed url: " + urlStr, e);
            return null;
        }
        
        StringBuffer relativePath = new StringBuffer(url.getHost()).append(fileSeparator);
        relativePath.append(url.getPort()).append(url.getPath());
        File f = new File(persistentFileCachePath, relativePath.toString());
        if (log.isDebugEnabled()) {
            log.debug("Persistent file path for url="+urlStr + " is '" + f.getAbsolutePath() + "'.");
        }
        return f;
    }

    protected String fetchContent(String url) throws HttpException, IOException {
        HttpMethod method = null;
        
        if (log.isDebugEnabled()) {
            log.debug("Fetching content for url: " + url);
        }
        
        //execute the method
        String responseBody = null;
        try{
            method = new GetMethod(url);
            method.setFollowRedirects(true);
            httpClient.executeMethod(method);
            responseBody = method.getResponseBodyAsString();
        } catch (HttpException he) {
            log.error("Http error connecting to '" + url + "'", he);
            throw he;
        } catch (IOException ioe){
            log.error("Unable to connect to '" + url + "'", ioe);
            throw ioe;
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Content for url: " + url + " is:\n"+responseBody);
        }
        return responseBody;
    }
    
    protected String getPreviousVersion(String url) {
        File filePath = null;
        try {
            filePath = getFilePath(url);
            if (log.isDebugEnabled()) {
                log.debug("Getting previous version for url="+url);
            }
            StringBuffer content = new StringBuffer();
            FileReader reader = new FileReader(filePath);
            char[] buf = new char[1024];
            int numRead;
            
            while ((numRead = reader.read(buf, 0, 1024))>=0) {
                content.append(new String(buf, 0, numRead));
            }
            if (log.isDebugEnabled()) {
                log.debug("Previous version content for url="+url+" is:\n"+content);
            }
            return content.toString();
        } catch (FileNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Previous version not found for url="+url, e);
            }
        } catch (IOException e) {
            log.error("Failed reading file="+(filePath != null ? filePath.getAbsolutePath() : "NULL"), e);
        }
        
        return null;
    }


    public void afterPropertiesSet() throws Exception {
        if (persistentFileCachePath != null && !new File(persistentFileCachePath).exists()) {
            throw new ServletException("Directory '" + persistentFileCachePath + "' does not exist.");
        }
        if (cacheWrapper == null) {
            throw new RuntimeException("cacheWrapper not set");
        }
    }
    
    public String getPersistentFileCachePath() {
        return persistentFileCachePath;
    }

    public void setPersistentFileCachePath(String persistentFileCachePath) {
        if (persistentFileCachePath == null) {
            if (log.isWarnEnabled()) {
                log.warn("persistentFileCachePath init-param is not set in web descriptor.");
            }
        } else {
            this.persistentFileCachePath = persistentFileCachePath;
        }
    }

    public CacheWrapper getCacheWrapper() {
        return cacheWrapper;
    }

    public void setCacheWrapper(CacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
