package org.wgs.core;

import java.security.Principal;
import org.wgs.entity.User;
import org.wgs.wamp.WampConnectionState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import org.wgs.wamp.WampSocket;
import org.wgs.entity.UserId;
import org.wgs.util.Storage;


public class Client 
{
    private User user;
    private String accessToken;
    private String refreshToken;
    private WampSocket socket;
    private Map<String,Group> groups = new ConcurrentHashMap<String,Group>();


    public String getSessionId() {
        return socket.getSessionId();
    }
    
    /**
     * @return the client
     */
    public WampSocket getSocket() {
        return socket;
    }

    /**
     * @param client the client to set
     */
    public void setSocket(WampSocket socket) {
        this.socket = socket;
    }


    public User getUser()
    {
        User user = null;
        Principal principal = socket.getUserPrincipal();
        if(principal != null) {
            if(principal instanceof User) {
                user = (User)principal;
            } else {
                String principalName = principal.getName();
                if(this.user == null) {
                    EntityManager manager = Storage.getEntityManager();
                    UserId userId = new UserId(User.LOCAL_USER_DOMAIN, principalName);
                    this.user = manager.find(User.class, userId);
                    manager.close();                    
                }
                user = this.user;
            }
        }
        return user;
    }
    
    
    /**
     * @return the groups
     */
    public Map<String,Group> getGroups() {
        return groups;
    }

    /**
     * @param groups the groups to set
     */
    public void addGroup(Group group) {
        groups.put(group.getGid(), group);
    }
    
    public void removeGroup(Group group)
    {
        groups.remove(group.getGid());
    }

    /**
     * @return the accessToken
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @param accessToken the accessToken to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * @return the refreshToken
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * @param refreshToken the refreshToken to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    

}
