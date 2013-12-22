package org.wgs.wamp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.wgs.util.MessageBroker;


public class WampModule 
{
    private WampApplication app;
    private HashMap<String,Method> rpcs;
    
    public WampModule(WampApplication app) {
        String moduleName = app.normalizeModuleName(getModuleName());
        this.app = app;
        rpcs = new HashMap<String,Method>();
        for(Method method : this.getClass().getMethods()) {
            WampRPC rpc = method.getAnnotation(WampRPC.class);
            if(rpc != null) {
                String name = rpc.name();
                if(name.length() == 0) name = method.getName();
                rpcs.put(moduleName + name, method);
            }
        }
        
    }
    
    public WampApplication getWampApplication()
    {
        return app;
    }
    
    public String getModuleName() 
    {
        String retval = "";
        WampModuleName ns = this.getClass().getAnnotation(WampModuleName.class);
        if(ns != null) retval = ns.value();
        else retval = this.getClass().getPackage().getName();
        return retval;
    }
    
    public void onConnect(WampSocket clientSocket) throws Exception { }
    
    public void onDisconnect(WampSocket clientSocket) throws Exception { }

    @SuppressWarnings("unchecked")
    public Object onCall(WampCallController task, WampSocket clientSocket, String methodName, ArrayNode args, WampCallOptions options) throws Exception 
    {
        ObjectMapper mapper = new ObjectMapper();
        Method method = rpcs.get(methodName);
        if(method != null) {
            int argCount = 0;
            ArrayList params = new ArrayList();
            for(Class paramType : method.getParameterTypes()) {
                if(paramType.isInstance(clientSocket)) {  // WampSocket parameter info
                    params.add(clientSocket);
                } else if(paramType.isInstance(app)) {    // WampApplication parameter info
                    params.add(app);
                } else if(WampCallController.class.isAssignableFrom(paramType)) {
                    params.add(task);                    
                } else if(WampCallOptions.class.isAssignableFrom(paramType)) {
                    params.add(options);
                } else if(ArrayNode.class.isAssignableFrom(paramType)) {
                    params.add(args);  // TODO: only from argCount to args.size()
                    argCount = args.size();
                } else {
                    JsonNode val = args.get(argCount++);
                    if(val == null || val instanceof NullNode) params.add(null);
                    else params.add(mapper.readValue(val, paramType));
                }
            }
            return method.invoke(this, params.toArray());
        }

        throw new WampException(WampException.WAMP_GENERIC_ERROR_URI, "Method not implemented: " + methodName);
    }
    
    public void onSubscribe(WampSocket clientSocket, WampTopic topic, WampSubscriptionOptions options) throws Exception { 
        WampSubscription subscription = topic.getSubscription(clientSocket.getSessionId());
        if(subscription == null) subscription = new WampSubscription(clientSocket, topic.getURI(), options);

        if(subscription.refCount(+1) > 1) {
            subscription.getOptions().updateOptions(options);
        } else {
            long sinceN = 0L;       // options.getSinceN();
            long sinceTime = 0L;    // options.getSinceTime();
            MessageBroker.subscribeMessageListener(topic, sinceTime, sinceN);
            topic.addSubscription(subscription);
            clientSocket.addSubscription(subscription);
            if(options != null && options.hasMetaEvents()) {
                WampServices.publishMetaEvent(topic, WampMetaTopic.OK, null, clientSocket);
                
                if(options.hasEventsEnabled()) {
                    WampServices.publishMetaEvent(topic, WampMetaTopic.JOINED, subscription.toJSON(), null);
                }
            }
        }
    }

    public void onUnsubscribe(WampSocket clientSocket, WampTopic topic) throws Exception { 
        WampSubscription subscription = topic.getSubscription(clientSocket.getSessionId());
        if(subscription.refCount(-1) <= 0) {
            WampSubscriptionOptions options = subscription.getOptions();
            if(options!=null && options.hasMetaEvents() && options.hasEventsEnabled()) {
                ObjectNode metaevent = subscription.toJSON();
                WampServices.publishMetaEvent(topic, WampMetaTopic.LEFT, metaevent, null);
            }
            topic.removeSubscription(subscription.getSocket().getSessionId());
            clientSocket.removeSubscription(subscription.getTopicUriOrPattern());
            
            if(topic.getSubscriptionCount() == 0) {
                MessageBroker.unsubscribeMessageListener(topic);
            }
        }
    }
    
    public void onPublish(WampSocket clientSocket, WampTopic topic, ArrayNode request) throws Exception 
    {
        WampPublishOptions options = new WampPublishOptions();
        JsonNode event = request.get(2);
        
        if(request.get(0).asInt() == 66) {
            // WAMP v2
            options.init(request.get(3));
            if(options.hasExcludeMe()) {
                Set<Long> excludedSet = options.getExcluded();
                if(excludedSet == null) excludedSet = new HashSet<Long>();
                excludedSet.add(clientSocket.getSessionId());
            }
        } else {
            // WAMP v1
            if(request.size() == 4) {
                // Argument 4 could be a BOOLEAN(excludeMe) or JSONArray(excludedIds)
                try {
                    boolean excludeMe = request.get(3).asBoolean();
                    options.setExcludeMe(excludeMe);                    
                    if(excludeMe) {
                        HashSet<Long> excludedSet = new HashSet<Long>();
                        excludedSet.add(clientSocket.getSessionId());
                    }
                } catch(Exception ex) {
                    HashSet<Long> excludedSet = new HashSet<Long>();
                    ArrayNode excludedArray = (ArrayNode)request.get(3);
                    for(int i = 0; i < excludedArray.size(); i++) {
                        excludedSet.add(excludedArray.get(i).asLong());
                    }
                    options.setExcluded(excludedSet);
                }
            } else if(request.size() == 5) {
                HashSet<Long> excludedSet = new HashSet<Long>();
                HashSet<Long> eligibleSet = new HashSet<Long>();
                ArrayNode excludedArray = (ArrayNode)request.get(3);
                for(int i = 0; i < excludedArray.size(); i++) {
                    excludedSet.add(excludedArray.get(i).asLong());
                }
                ArrayNode eligibleArray = (ArrayNode)request.get(4);
                for(int i = 0; i < eligibleArray.size(); i++) {
                    eligibleSet.add(eligibleArray.get(i).asLong());
                }
                options.setExcluded(excludedSet);
                options.setEligible(eligibleSet);
            }
        }
        
        WampServices.publishEvent(clientSocket.getSessionId(), topic, event, options);
        
    }

    
}
