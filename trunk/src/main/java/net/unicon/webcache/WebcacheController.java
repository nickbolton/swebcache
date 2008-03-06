package net.unicon.webcache;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

public class WebcacheController extends AbstractCacheController {
    
    private Log log = LogFactory.getLog(getClass());
        
    public WebcacheController() {
        super();
    }

    public ModelAndView handleRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        String url = req.getParameter("url");
        CacheUtility cacheUtility = getCacheUtility();
        CacheWrapper cacheWrapper = cacheUtility.getCacheWrapper();
        Element element = cacheWrapper.get(url);
        if (element == null) {
            if (log.isDebugEnabled()) {
                log.debug("cache miss for url: " + url);
            }
            String content = cacheUtility.fetchContent(url);
            if (content != null) {
                element = new Element(url, content);
                cacheWrapper.put(element);
                cacheUtility.storePreviousVersion(url, content);
            } else {
                content = cacheUtility.getPreviousVersion(url);
                if (content != null) {
                    element = new Element(url, content);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("cache hit for url: " + url);
            }
        }
        
        if (element != null) {
            resp.getWriter().print(element.getValue());
        } else {
            resp.setStatus(404);
        }

        return null;
    }
    
}