/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fortiss.uaserver.msb;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.collect.Maps;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity for managing server registry and monitoring expiration time.
 *
 * @author cheng
 */
public class ServerRegistrationManagement {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    Timer timer;

    /**
     * Maximum interval (in seconds) between two RegisterServerRequest for the same server.
     */
    private static final long EXPIRED_TIME_IN_SEC = 3600l;

    /**
     * Period (in seconds) for executing the server expiration check.
     */
    private static final int PERIOD = 60;

    /**
     * Map for keeping track of serverName and the corresponding application description.
     */
    private static final Map<String, ApplicationDescription> registeredApplicationDescription = Maps.newConcurrentMap();

    /**
     * Map for keeping track of serverName and the last server registry timestamp.
     */
    private static final Map<String, Date> serverExpirationMap = Maps.newConcurrentMap();

    public ServerRegistrationManagement() {
        timer = new Timer();
        // Trigger floor sweeping periodically. 
        timer.schedule(new ServerExpirationManager(), 0, PERIOD * 1000);
    }

    class ServerExpirationManager extends TimerTask {
        public void run() {
            clearExipredElementsFromMap(serverExpirationMap);
        }
    }

    public void addElement(String serverUri, ApplicationDescription ad) {

        synchronized (serverExpirationMap) {
            if (!getRegisteredApplicationDescription().keySet().contains(serverUri)) {
                getRegisteredApplicationDescription().put(serverUri, ad);
                serverExpirationMap.put(serverUri, new Date());
                logger.info("+++++++++++ Server added: " + serverUri + "\n");
            } else {
                serverExpirationMap.put(serverUri, new Date());
                logger.info("+++++++++++ Server expiration updated: " + serverUri + "\n");
            }
        }
    }

    public void removeElement(String serverUri) {
        synchronized (serverExpirationMap) {
            synchronized (registeredApplicationDescription) {
                serverExpirationMap.remove(serverUri);
                registeredApplicationDescription.remove(serverUri);
            }
        }
    }

    private void clearExipredElementsFromMap(Map<String, Date> map) {
        synchronized (map) {
            Date currentTime = new Date();
            Date actualExpiredTime = new Date();

            actualExpiredTime.setTime(currentTime.getTime() - EXPIRED_TIME_IN_SEC * 1000l);

            Iterator<Entry<String, Date>> expirationMapIterator = map.entrySet().iterator();
            while (expirationMapIterator.hasNext()) {
                Entry<String, Date> entry = expirationMapIterator.next();
                Date lastRegisterTime = entry.getValue();

                if (lastRegisterTime.compareTo(actualExpiredTime) < 0) {
                    logger.info("----------- Server deleted (due to expiration): " + entry.getKey());
                    expirationMapIterator.remove();
                    synchronized (registeredApplicationDescription) {
                        registeredApplicationDescription.remove(entry.getKey());
                    }
                }
            }
        }
    }

    /**
     * @return the registeredApplicationDescription
     */
    public Map<String, ApplicationDescription> getRegisteredApplicationDescription() {
        return registeredApplicationDescription;
    }

}
