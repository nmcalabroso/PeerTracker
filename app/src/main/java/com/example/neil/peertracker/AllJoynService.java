package com.example.neil.peertracker;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

import org.alljoyn.bus.BusAttachment;

public class AllJoynService extends IntentService {

    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment mBus;

    public AllJoynService() {
        super("AllJoynService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public void onCreate() {
    }
}
