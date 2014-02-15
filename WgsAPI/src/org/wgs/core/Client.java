package org.wgs.core;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;

import org.wgs.entity.User;
import org.wgs.entity.UserId;
import org.wgs.util.Storage;
import org.wgs.wamp.WampSocket;


public class Client 
{
    private User user;
    private WampSocket socket;
    private Map<String,Group> groups = new ConcurrentHashMap<String,Group>();


    public Long getSessionId() {
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
                    UserId userId = new UserId(User.LOCAL_USER_DOMAIN, principalName);
                    this.user = Storage.findEntity(User.class, userId);
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

}
