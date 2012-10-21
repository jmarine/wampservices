/**
 * Servlet implementation for WAMP services.
  *
 * @author Jordi Mariné Fort
 */

package com.github.jmarine.wampservices;

import com.sun.grizzly.websockets.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;


public class WampServlet extends HttpServlet 
{
    private static final Logger logger = Logger.getLogger(WampServlet.class.toString());
    
    private String contextPath = "/wsgservice";
    private WampApplication wampApplication = null;

    @Override
    public void init(ServletConfig config) throws ServletException  {
        super.init(config);

        Properties appConfig = new Properties();
        Enumeration e = config.getInitParameterNames();
        while(e.hasMoreElements()) {
            String name = (String)e.nextElement();
            String value = config.getInitParameter(name);
            appConfig.put(name, value);
        }
            
        try {
            String uri = config.getInitParameter("uri");
            if(uri == null) throw new ServletException("ServletInitParameter uri is not defined");
            
            boolean topicWildcardsEnabled = false;
            String enableWildcards = config.getInitParameter("enableTopicWildcards");
            if((enableWildcards != null) && (enableWildcards.toUpperCase().equals("TRUE"))) topicWildcardsEnabled = true;
            wampApplication = new WampApplication(appConfig, contextPath, topicWildcardsEnabled);
            
            String topics = config.getInitParameter("topics");
            if(topics != null) {
                StringTokenizer tokenizer = new StringTokenizer(topics,",");
                while(tokenizer.hasMoreTokens()) {
                    String topic = tokenizer.nextToken();
                    wampApplication.createTopic(topic, null);
                }
            }
            
            String modules = config.getInitParameter("modules");
            if(modules != null) {
                StringTokenizer tokenizer = new StringTokenizer(modules,",");
                while(tokenizer.hasMoreTokens()) {
                    String module = tokenizer.nextToken();
                    wampApplication.registerWampModule(Class.forName(module));
                }
            }            
            
            WebSocketEngine.getEngine().register(wampApplication);
            logger.log(Level.CONFIG, "WsgServlet: uri={0}", new Object[]{contextPath});
        } catch(Exception ex) {
            throw new ServletException("WampServlet error", ex);
        }
    }

    @Override
    public void destroy() {
        WebSocketEngine.getEngine().unregister(wampApplication);
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=iso-8859-1");
        resp.getWriter().write(contextPath);
        resp.getWriter().flush();
    }
}
