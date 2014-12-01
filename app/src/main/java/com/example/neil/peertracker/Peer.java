package com.example.neil.peertracker;

import org.alljoyn.services.common.BusObjectDescription;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    public String getAnnounce() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusName: "+busName+"\n\n");
        sb.append("Port: "+port+"\n\n");
        if(aboutMap == null){
            sb.append("About map:\n");
            sb.append("About map is null\n");
            sb.append("\n");
        }
        else{
            Set<String> set = aboutMap.keySet();
            sb.append("About map:\n");
            Iterator<String> iterator = set.iterator();
            while (iterator.hasNext()){
                String current = iterator.next();
                Object value = aboutMap.get(current);
                sb.append(current+" : "+value.toString()+"\n");
            }
            sb.append("\n");
        }
        sb.append("Bus Object Description:\n");
        for(int i = 0; i < interfaces.length; i++){
            sb.append(busObjectDescriptionString(interfaces[i]));
            if(i != interfaces.length-1)
                sb.append("\n");
        }
        return sb.toString();
    }

    private String busObjectDescriptionString(BusObjectDescription bus) {
        String s = "";
        s += "path: "+bus.getPath()+"\n";
        s += "interfaces: ";
        String[] tmp = bus.getInterfaces();
        for (int i = 0; i < tmp.length; i++){
            s += tmp[i];
            if(i != tmp.length-1)
                s += ",";
            else
                s += "\n";
        }
        return s;
    }
}
