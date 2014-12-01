package com.example.neil.peertracker;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import org.alljoyn.about.AboutKeys;
import org.alljoyn.about.AboutService;
import org.alljoyn.about.AboutServiceImpl;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.alljoyn.DaemonInit;
import org.alljoyn.onboarding.OnboardingService;
import org.alljoyn.onboarding.client.OnboardingClientImpl;
import org.alljoyn.onboarding.transport.OBLastError;
import org.alljoyn.onboarding.transport.OnboardingTransport;
import org.alljoyn.onboarding.transport.ScanInfo;
import org.alljoyn.services.android.security.AuthPasswordHandler;
import org.alljoyn.services.android.security.SrpAnonymousKeyListener;
import org.alljoyn.services.common.AnnouncementHandler;
import org.alljoyn.services.common.BusObjectDescription;
import org.alljoyn.services.common.utils.TransportUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/* Main Application */

public class PeerTracker extends Application implements AuthPasswordHandler {
    public static final String TAG = "OnBoardingClient";
    public static final String TAG_PASSWORD = "OnboardingApplication_password";

    private BusAttachment m_Bus;
    private NetworkManager m_networkManager;
    private HashMap<String, Peer> m_devicesMap;
    private AboutService m_aboutClient;
    private Peer m_currentPeer;
    private String m_channelName;
    private BroadcastReceiver m_receiver;
    private static final String DAEMON_QUIET_PREFIX = "quiet@";
    private OnboardingClientImpl m_onboardingClient;

    static {
        try {
            System.loadLibrary("alljoyn_java");
        } catch (Exception e) {
            System.out.println("Cannot load library: AllJoyn");
        }
    }

    public NetworkManager getNetworkManager() {
        return this.m_networkManager;
    }

    public void makeToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void setRealmName(String channelName) {
        m_channelName = channelName;
    }

    public void doConnect() {
        boolean b = DaemonInit.PrepareDaemon(getApplicationContext());
        String ss = getPackageName();
        m_Bus = new BusAttachment(ss, BusAttachment.RemoteMessage.Receive);
        Status status = m_Bus.connect();
        Log.d(TAG, "bus.connect status: " + status);

        m_Bus.setDaemonDebug("ALL", 7);
        m_Bus.setLogLevels("ALL=7");
        m_Bus.useOSLogging(true);

        String keyStoreFileName = null;
        try {
            AnnouncementHandler receiver = new AnnouncementHandler() {
                @Override
                public void onAnnouncement(String busName, short port, BusObjectDescription[] interfaces, Map<String, Variant> aboutMap) {

                    Map<String, Object> newMap = new HashMap<String, Object>();
                    try {
                        newMap = TransportUtil.fromVariantMap(aboutMap);
                        String deviceId = (newMap.get(AboutKeys.ABOUT_APP_ID).toString());
                        String deviceFriendlyName = (String) newMap.get(AboutKeys.ABOUT_DEVICE_NAME);
                        addDevice(deviceId, busName, port, deviceFriendlyName, interfaces, newMap);
                    } catch (BusException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDeviceLost(String serviceName) {
                    removeDevice(serviceName);
                }
            };

            m_aboutClient = AboutServiceImpl.getInstance();
            m_aboutClient.startAboutClient(m_Bus);
            m_aboutClient.addAnnouncementHandler(receiver, new String[] { OnboardingTransport.INTERFACE_NAME });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // request the name
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        String DAEMON_NAME = m_channelName;
        Status reqStatus = m_Bus.requestName(DAEMON_NAME, flag);
        if (reqStatus == Status.OK) {
            // advertise the name
            // advertise the name with a quite prefix for TC to find it
            Status adStatus = m_Bus.advertiseName(DAEMON_QUIET_PREFIX + DAEMON_NAME, SessionOpts.TRANSPORT_ANY);
            if (adStatus != Status.OK) {
                m_Bus.releaseName(DAEMON_NAME);
            }
        }

        // set keyListener
        keyStoreFileName = getApplicationContext().getFileStreamPath("alljoyn_keystore").getAbsolutePath();
        if (keyStoreFileName.length() > 0) {
            SrpAnonymousKeyListener authListener = new SrpAnonymousKeyListener(PeerTracker.this, null, new String[] { "ALLJOYN_SRP_KEYX", "ALLJOYN_ECDHE_PSK", "ALLJOYN_PIN_KEYX" });
            Status authStatus = m_Bus.registerAuthListener(authListener.getAuthMechanismsAsString(), authListener, keyStoreFileName);
        }
    }

    public void doDisconnect() {
        try {
            if (m_aboutClient != null)
                m_aboutClient.stopAboutClient();
            if (m_Bus != null) {
                m_Bus.clearKeyStore();
                m_Bus.cancelAdvertiseName(DAEMON_QUIET_PREFIX + m_channelName, SessionOpts.TRANSPORT_ANY);
                m_Bus.releaseName(m_channelName);
                m_Bus.disconnect();
                m_Bus = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDevice(String deviceId, String busName, short port, String deviceFriendlyName, BusObjectDescription[] interfaces, Map<String, Object> aboutMap) {
        Peer oldDevice = m_devicesMap.get(deviceId);

        if (oldDevice != null) {
            if (!oldDevice.busName.equals(busName)) {
                oldDevice.password = SrpAnonymousKeyListener.DEFAULT_PINCODE;
            }

            oldDevice.busName = busName;
            oldDevice.aboutMap = aboutMap;
            oldDevice.displayName = deviceFriendlyName;
            oldDevice.port = port;
            oldDevice.interfaces = interfaces;
            oldDevice.updateSupportedDevices();

        } else {
            // add the device to the map
            Peer sad = new Peer(deviceId, busName, deviceFriendlyName,
                    SrpAnonymousKeyListener.DEFAULT_PINCODE, port, interfaces, aboutMap);
            m_devicesMap.put(deviceId, sad);
        }
        // notify the activity to come and get it
        Intent intent = new Intent(Keys.Actions.ACTION_DEVICE_FOUND);
        Bundle extras = new Bundle();
        extras.putString(Keys.Extras.EXTRA_DEVICE_ID, deviceId);
        intent.putExtras(extras);
        sendBroadcast(intent);
    }

    private void removeDevice(String busName) {
        Collection<Peer> devices = m_devicesMap.values();
        Object[] array = devices.toArray();
        for (Object anArray : array) {
            Peer d = (Peer) anArray;
            if (d.busName.equals(busName)) {
                m_devicesMap.remove(d.uuid);
            }
        }
        Intent intent = new Intent(Keys.Actions.ACTION_DEVICE_LOST);
        Bundle extras = new Bundle();
        extras.putString(Keys.Extras.EXTRA_BUS_NAME, busName);
        intent.putExtras(extras);
        sendBroadcast(intent);
    }

    private void updateTheUiAboutError(String error) {
        Intent intent = new Intent(Keys.Actions.ACTION_ERROR);
        intent.putExtra(Keys.Extras.EXTRA_ERROR, error);
        sendBroadcast(intent);
    }

    public Peer getDevice(String deviceId) {
        return m_devicesMap.get(deviceId);
    }

    public void showAlert(Context context, String errorMsg) {

        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Error");
        alert.setMessage(errorMsg);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    public void startSession(Peer device) {
        m_currentPeer = device;
        m_onboardingClient = new OnboardingClientImpl(m_currentPeer.busName, m_Bus, null, m_currentPeer.port);

        Status status = m_onboardingClient.connect();
    }

    public void endSession() {
        if (m_onboardingClient != null) {
            m_onboardingClient.disconnect();
        }
    }

    public Short getOnboardingVersion() {
        short onboardingVersion = -1;
        try {
            if (m_currentPeer.supportOnboarding) {

                if (!m_onboardingClient.isConnected()) {
                    m_onboardingClient.connect();
                }
                onboardingVersion = m_onboardingClient.getVersion();
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("GET ONBOARDING VERSION: Exception: " + e.toString());
        }
        return Short.valueOf(onboardingVersion);
    }

    public OBLastError getLastError() {
        OBLastError m_lastError = new OBLastError();
        m_lastError.setErrorCode((short) -1);
        m_lastError.setErrorMessage("unKnown");

        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            m_lastError = m_onboardingClient.GetLastError();

        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("GET LAST ERROR: Exception: " + e.toString());
        }
        return m_lastError;
    }

    public Short getState() {
        short state = -1;
        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            state = m_onboardingClient.getState();

        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("GET STATE: Exception: " + e.toString());
        }
        return state;
    }

    public ScanInfo getScanInfo() {

        ScanInfo scanInfo = null;
        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            scanInfo = m_onboardingClient.getScanInfo();

        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("GET SCAN INFO: Exception: " + e.toString());
        }
        return scanInfo;
    }

    public void configureNetwork(String networkName, String networkPassword, short networkAuthType) {

        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            OnboardingTransport.ConfigureWifiMode mode = m_onboardingClient.configureWiFi(networkName,
                    networkPassword, OnboardingService.AuthType.getAuthTypeById(networkAuthType));
        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("CONFIGURE NETWORK: Exception: " + e.toString());
        }
    }

    public void connectNetwork() {
        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            m_onboardingClient.connectWiFi();
        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("CONNECT NETWORK: Exception: " + e.toString());
        }
    }

    public void offboard() {
        try {
            if (!m_onboardingClient.isConnected()) {
                m_onboardingClient.connect();
            }
            m_onboardingClient.offboard();
        } catch (Exception e) {
            e.printStackTrace();
            updateTheUiAboutError("OFFBOARDING: " + e.getMessage());
        }
    }

    public void setPassword(String peerName, char[] password) {
        Collection<Peer> devices = m_devicesMap.values();
        Iterator<Peer> iterator = devices.iterator();
        for (; iterator.hasNext();) {
            Peer softAPDetails = iterator.next();
            if (softAPDetails.busName.equals(peerName)) {
                softAPDetails.password = password;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();

        m_networkManager = new NetworkManager(getApplicationContext());
        m_devicesMap = new HashMap<String, Peer>();

        // Receiver
        m_receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    String str = "";

                    if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        str = wifiInfo.getSSID();
                    } else {
                        str = networkInfo.getState().toString().toLowerCase(Locale.getDefault());
                    }
                    Intent networkIntent = new Intent(Keys.Actions.ACTION_CONNECTED_TO_NETWORK);
                    networkIntent.putExtra(Keys.Extras.EXTRA_NETWORK_SSID, str);
                    sendBroadcast(networkIntent);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(m_receiver, filter);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(m_receiver);
    }

    @Override
    public void completed(String mechanism, String authPeer, boolean authenticated) {
        if (!authenticated) {
            Intent intent = new Intent(Keys.Actions.ACTION_PASSWORD_IS_INCORRECT);
            sendBroadcast(intent);
        }
    }

    @Override
    public char[] getPassword(String peerName) {
        Peer peer = null;
        if (peerName != null) {
            Collection<Peer> devices = m_devicesMap.values();
            Iterator<Peer> iterator = devices.iterator();
            for (; iterator.hasNext();) {
                peer = iterator.next();
                if (peer.busName.equals(peerName)) {
                    char[] password = peer.password;
                    if (password != null) {
                        return password;
                    }
                }
            }
        }
        return SrpAnonymousKeyListener.DEFAULT_PINCODE;
    }
}
