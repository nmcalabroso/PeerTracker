package com.example.neil.peertracker;

import org.alljoyn.services.common.BusObjectDescription;

import java.util.Map;

/*
This class represents a device
 */
public class Peer {

    public String uuid;
    public String busName;
    public String displayName;
    public char[] password;

    public short port;
    public boolean supportOnboarding = false;

    public BusObjectDescription[] interfaces;
    public Map<String, Object> aboutMap;

    public Peer(String uuid, String busName, String displayName,
                char[] password, short port, BusObjectDescription[] interfaces,
                Map<String, Object> aboutMap) {

        this.uuid = uuid;
        this.busName = busName;
        this.displayName = displayName;
        this.password = password;
        this.port = port;
        this.interfaces = interfaces;
        this.aboutMap = aboutMap;
        updateSupportedDevices();
    }

    public void updateSupportedDevices() {
        if(interfaces != null) for (BusObjectDescription temp : interfaces) {
            String[] supportedInterfaces = temp.getInterfaces();
            for (String interface1 : supportedInterfaces) {
                if (interface1.startsWith("org.alljoyn.Onboarding")) {
                    this.supportOnboarding = true;
                }
            }
        }
    }
}
