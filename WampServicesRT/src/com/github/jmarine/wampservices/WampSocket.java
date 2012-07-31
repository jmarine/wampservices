package com.github.jmarine.wampservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.grizzly.websockets.DefaultWebSocket;
import com.sun.grizzly.websockets.ProtocolHandler;
import com.sun.grizzly.websockets.WebSocketException;
import com.sun.grizzly.websockets.WebSocketListener;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class WampSocket extends DefaultWebSocket
{
    // TODO: topic subscription management
    
    private static final Logger logger = Logger.getLogger(WampSocket.class.toString());
    
    private WampApplication app;
    private String sessionId;
    private Map    sessionData;
    private Map<String,String> prefixes;
    private Map<String,WampTopic> topics;

    public WampSocket(WampApplication app,
                        ProtocolHandler protocolHandler,
                         //HttpRequestPacket request,
                         WebSocketListener... listeners) 
    {
        super(protocolHandler, listeners);
        this.app    = app;
        sessionId   = UUID.randomUUID().toString();
        sessionData = new ConcurrentHashMap();
        topics      = new ConcurrentHashMap<String,WampTopic>();        
        prefixes    = new HashMap<String,String>();
    }

    /**
     * Get the session ID
     * @return the user name
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get the session data 
     * @return the user name
     */
    public Map getSessionData() {
        return sessionData;
    }


    public void registerPrefixURL(String prefix, String url)
    {
        prefixes.put(prefix, url);	
    }
    
    public String getPrefixURL(String prefix)
    {
        return prefixes.get(prefix);
    }

    public void addTopic(WampTopic topic)
    {
        topics.put(topic.getURI(), topic);
    }
    
    public void removeTopic(WampTopic topic)
    {
        topics.remove(topic.getURI());
    }
    
    public Map<String,WampTopic> getTopics()
    {
        return topics;
    }
    
    
    public String normalizeURI(String curie) {
        int curiePos = curie.indexOf(":");
        if(curiePos != -1) {
            String prefix = curie.substring(0, curiePos);
            String baseURI = getPrefixURL(prefix);
            if(baseURI != null) curie = baseURI + curie.substring(curiePos+1);
        }
        return curie;
    }    

    /**
     * Send the message in JSON encoding acceptable by browser's javascript.
     *
     * @param user the user name
     * @param text the text message
     */
    protected void sendSafe(String msg) {
        try {
            super.send(msg);
        } catch (WebSocketException e) {
            logger.log(Level.SEVERE, "Removing wamp client: " + e.getMessage(), e);
            close(PROTOCOL_ERROR, e.getMessage());
        }
    }
    
    
    protected void sendCallResult(String callID, JSONArray args)
    {
        StringBuilder response = new StringBuilder();
        if(args == null) {
            args = new JSONArray();
            args.put((String)null);
        }

        response.append("[");
        response.append("3");
        response.append(",");
        response.append(app.encodeJSON(callID));
        for(int i = 0; i < args.length(); i++) {
            response.append(",");
            try { 
                Object obj = args.get(i); 
                if(obj instanceof String) {
                    response.append("\"");
                    response.append(app.encodeJSON((String)obj));
                    response.append("\"");
                } else {
                    response.append(obj); 
                }
            }
            catch(Exception ex) { response.append("null"); }
        }
        response.append("]");
        sendSafe(response.toString());
    }    
    
    
    protected void sendCallError(String callID, String errorURI, String errorDesc, Object errorDetails)
    {
        if(errorURI == null) errorURI = WampException.WAMP_GENERIC_ERROR_URI;
        if(errorDesc == null) errorDesc = "";

        StringBuilder response = new StringBuilder();
        response.append("[");
        response.append("4");
        response.append(",");
        response.append(app.encodeJSON(callID));

        response.append(",");
        response.append(app.encodeJSON(errorURI));
        response.append(",");
        response.append(app.encodeJSON(errorDesc));
        
        if(errorDetails != null) {
            response.append(",");
            if(errorDetails instanceof String) {
                response.append("\"");
                response.append(app.encodeJSON((String)errorDetails));
                response.append("\"");
            } else {
                response.append(errorDetails.toString());
            }
        }

        response.append("]");
        sendSafe(response.toString());
    }    
    
    
    /**
     * Broadcasts the event to subscribed sockets.
     */
    public void publishEvent(WampTopic topic, JSONObject event, boolean excludeMe) {
        logger.log(Level.INFO, "Preparation for broadcasting to {0}: {1}", new Object[]{topic.getURI(),event});
        Set<String> excludedSet = new HashSet<String>();
        if(excludeMe) excludedSet.add(this.getSessionId());
        publishEvent(topic, event, excludedSet, null);
    }
    
    public void publishEvent(WampTopic topic, JSONObject event, Set<String> excluded, Set<String> eligible) {
        logger.log(Level.INFO, "Broadcasting to {0}: {1}", new Object[]{topic.getURI(),event});
        String msg = "[8,\"" + topic.getURI() + "\", " + event.toString() + "]";
        
        if(eligible == null)  eligible = topic.getSocketIds();
        for (String cid : eligible) {
            if((excluded==null) || (!excluded.contains(cid))) {
                WampSocket socket = topic.getSocket(cid);
                if(socket != null && socket.isConnected() && !excluded.contains(cid)) {
                    try { 
                        WampModule module = app.getWampModule(topic.getBaseURI());
                        if(module != null) module.onPublish(socket, topic, event); 
                        socket.sendSafe(msg);
                    } catch(Exception ex) {
                        logger.log(Level.SEVERE, "Error dispatching event publication to registered module", ex);
                    }          
                }
            }
        }
    }    

}
