package com.example.neil.peertracker;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

public class AllJoynService extends IntentService {

    public AllJoynService() {
        super("AllJoynService");
    }

    static {
        System.loadLibrary("alljoyn_java");
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
