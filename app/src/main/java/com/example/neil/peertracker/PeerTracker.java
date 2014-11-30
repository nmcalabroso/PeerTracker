package com.example.neil.peertracker;

import android.app.Application;
import android.os.HandlerThread;

import org.alljoyn.about.AboutService;
import org.alljoyn.bus.BusAttachment;

/* Main Application */

public class PeerTracker extends Application {
    public static final String TAG = "OnBoardingClient";
    public static final String TAG_PASSWORD = "OnboardingApplication_password";

    private BusAttachment m_Bus;
    private NetworkManager m_networkManager;
    private AboutService aboutService;
    private static final String DAEMON_QUIET_PREFIX = "quiet@";

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

    @Override
    public void onCreate() {
        super.onCreate();
        //HandlerThread busThread = new HandlerThread("BusHandler");
        //busThread.start();
    }


}
