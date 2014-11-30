package com.example.neil.peertracker;

import java.util.List;

import android.net.wifi.ScanResult;

public interface WifiManagerListener {
    public void OnScanResultComplete(List<ScanResult> results);
}
