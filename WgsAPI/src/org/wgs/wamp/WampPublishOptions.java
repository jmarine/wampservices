package org.wgs.wamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;


public class WampPublishOptions 
{
    private boolean   excludeMe;
    private Set<Long> excluded;
    private Set<Long> eligible;
    private boolean   discloseMe;
        
    public WampPublishOptions() { }
    public WampPublishOptions(WampDict node) { 
        init(node);
    }
    
    public void init(WampDict node) {
        
        if(node != null) {
            if(node.has("EXCLUDE_ME")) {
                setExcludeMe(node.get("exclude_me").asBoolean());
            }     
            
            if(node.has("IDENTIFY_ME")) {
                setDiscloseMe(node.get("disclose_me").asBoolean());
            }                 
        }
    }

    /**
     * @return the excludeMe
     */
    public boolean hasExcludeMe() {
        return excludeMe;
    }

    /**
     * @param excludeMe the excludeMe to set
     */
    public void setExcludeMe(boolean excludeMe) {
        this.excludeMe = excludeMe;
    }

    /**
     * @return the excluded
     */
    public Set<Long> getExcluded() {
        return excluded;
    }

    /**
     * @param excluded the excluded to set
     */
    public void setExcluded(Set<Long> excluded) {
        this.excluded = excluded;
    }

    /**
     * @return the eligible
     */
    public Set<Long> getEligible() {
        return eligible;
    }

    /**
     * @param eligible the eligible to set
     */
    public void setEligible(Set<Long> eligible) {
        this.eligible = eligible;
    }

    /**
     * @return the identifyMe
     */
    public boolean hasDiscloseMe() {
        return discloseMe;
    }

    /**
     * @param identifyMe the identifyMe to set
     */
    public void setDiscloseMe(boolean identifyMe) {
        this.discloseMe = identifyMe;
    }
    
    
}

