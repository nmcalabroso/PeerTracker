package com.example.neil.peertracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.util.Pair;

import org.alljoyn.onboarding.OnboardingService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkManager {
    final String INT_ENTERPRISEFIELD_NAME ="android.net.wifi.WifiConfiguration$EnterpriseField";
    private static final String TAG = "NetworkManager";
    private volatile WifiConfiguration targetWifiConfiguration = null;
    private Timer wifiTimeoutTimer = new Timer();
    static final String WEP_HEX_PATTERN = "[\\dA-Fa-f]+";
    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
    private Context m_context;
    private WifiManager m_wifi;
    private WifiManagerListener m_listener;
    private BroadcastReceiver m_scanner;

    public NetworkManager(Context context) {
        m_context = context;
        m_wifi = (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);
    }

    public void scanForNetwork(Context c, WifiManagerListener listener, String AJlookupPrefix) {
        m_listener = listener;
        m_context = c;
        m_wifi = (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);

        /* listen to Wi-Fi intents */
        m_scanner = new BroadcastReceiver() {
            // will get here after scan
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // === Current scans ===
                List<ScanResult> scans = m_wifi.getScanResults();

                // remove duplicate SSID with different BSSID ,
                if (scans != null){

                    // keep one item per SSID, the one with the strongest signal
                    HashMap<String, ScanResult> alljoynSoftAPs = new HashMap<String, ScanResult>();
                    for (ScanResult currentScan : scans){
                        ScanResult l=alljoynSoftAPs.get(currentScan.SSID);
                        if (l==null)
                        {
                            alljoynSoftAPs.put(currentScan.SSID, currentScan);
                        }else{
                            if (l.level<currentScan.level)
                            {
                                alljoynSoftAPs.put(currentScan.SSID, currentScan);
                            }
                        }

                    }

                    // sort list by level of Wi-Fi signal
                    List <ScanResult> list=new ArrayList<ScanResult>(alljoynSoftAPs.values());
                    Collections.sort(list, new Comparator<ScanResult>() {
                        public int compare(ScanResult o1, ScanResult o2) {
                            if (o1.level > o2.level)
                                return -1;
                            else if (o1.level < o2.level)
                                return 1;
                            else
                                return 0;
                        }
                    });
                    // listener callback
                    m_listener.OnScanResultComplete(list);

                }
            }
        };

        // register for Wi-Fi intents that will be generated by the scanning process
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        m_context.registerReceiver(m_scanner, filter);

        // start a scan
        m_wifi.startScan();
    }

    public String getCurrentNetworkSSID(){
        return m_wifi.getConnectionInfo().getSSID();
    }

    static String normalizeSSID(String ssid) {
        if (ssid != null && ssid.length() > 0 && ssid.startsWith("\"")) {
            ssid = ssid.replace("\"", "");
        }
        return ssid;
    }

    public static boolean isSsidEquals(String ssid1, String ssid2) {
        if (ssid1 == null || ssid1.length() == 0 || ssid2 == null || ssid2.length() == 0)
            return false;
        if (ssid1.startsWith("\"")){
            ssid1 = ssid1.replace("\"", "");
        }
        if (ssid2.startsWith("\"")){
            ssid2 = ssid2.replace("\"", "");
        }
        return ssid1.equals(ssid2);
    }

    public OnboardingService.AuthType getScanResultSecurity(String capabilities) {
        Log.i(TAG, "* getScanResultSecurity");

        if (capabilities.contains(OnboardingService.AuthType.WEP.name())) {
            return OnboardingService.AuthType.WEP;
        }
        else if (capabilities.contains("WPA")) {
            if (capabilities.contains("WPA2"))
                return OnboardingService.AuthType.WPA2_AUTO;
            else
                return OnboardingService.AuthType.WPA_AUTO;
        }
        return OnboardingService.AuthType.OPEN;

    }

    private OnboardingService.AuthType getSSIDAuthType(String ssid) {
        Log.d(TAG, "getSSIDAuthType SSID = " + ssid);
        if (ssid == null || ssid.length() == 0) {
            Log.w(TAG, "getSSIDAuthType given string was null");
            return null;
        }
        List<ScanResult> networks = m_wifi.getScanResults();
        for (ScanResult scan : networks) {
            if (ssid.equalsIgnoreCase(scan.SSID)) {
                OnboardingService.AuthType res = getScanResultSecurity(scan.capabilities);
                return res;
            }
        }
        return null;
    }

    private void connect(final WifiConfiguration wifiConfig, final int networkId, final long timeoutMsec) {

        Log.i(TAG, "connect  SSID=" + wifiConfig.SSID + " within " + timeoutMsec);
        boolean res;

        synchronized (this) {
            targetWifiConfiguration = wifiConfig;
        }

        // this is the application's Wi-Fi connection timeout
        wifiTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "Network Listener WIFI_TIMEOUT  when trying to connect to " + normalizeSSID(targetWifiConfiguration.SSID));
            }
        }, timeoutMsec);

        if (m_wifi.getConnectionInfo().getSupplicantState() == SupplicantState.DISCONNECTED) {
            m_wifi.disconnect();
        }

        res = m_wifi.enableNetwork(networkId, false);
        Log.d(TAG, "connect enableNetwork [false] status=" + res);
        res = m_wifi.disconnect();
        Log.d(TAG, "connect disconnect  status=" + res);

        // enabling a network doesn't guarantee that it's the one that Android
        // will connect to.
        // Selecting a particular network is achieved by passing 'true' here to
        // disable all other networks.
        // the side effect is that all other user's Wi-Fi networks become
        // disabled.
        // The recovery for that is enableAllWifiNetworks method.
        res = m_wifi.enableNetwork(networkId, true);
        Log.d(TAG, "connect enableNetwork [true] status=" + res);
        res = m_wifi.reconnect();
        m_wifi.setWifiEnabled(true);
    }

    public boolean connectToAP(String networkName, String passkey, String capabilities) {
        Log.i(TAG, "* connectToAP with capabilities");
        OnboardingService.AuthType securityMode = getScanResultSecurity(capabilities);
        return connectToAP(networkName,passkey, securityMode.getTypeId());
    }

    public boolean connectToAP(String ssid, String password, short authTypeCode) {
        OnboardingService.AuthType authType = OnboardingService.AuthType.getAuthTypeById(authTypeCode);

        if (password == null) password = "";

        final List<WifiConfiguration> wifiConfigs = m_wifi.getConfiguredNetworks();

        StringBuffer buff = new StringBuffer();
        for (WifiConfiguration w : wifiConfigs) {
            if (w.SSID != null) {
                w.SSID = normalizeSSID(w.SSID);
                if (w.SSID.length() > 1) {
                    buff.append(w.SSID).append(",");
                }
            }
        }

        int networkId = -1;

        for (WifiConfiguration w : wifiConfigs) {
            if (w.SSID != null && isSsidEquals(w.SSID, ssid)) {
                networkId = w.networkId;
                boolean res = m_wifi.removeNetwork(networkId);
                res = m_wifi.saveConfiguration();
                break;
            }
        }

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        OnboardingService.AuthType verrifiedWifiAuthType = getSSIDAuthType(ssid);
        if (verrifiedWifiAuthType != null) {
            authType = verrifiedWifiAuthType;
        }

        switch (authType) {
            case OPEN:
                wifiConfiguration.SSID = "\"" + ssid + "\"";
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                networkId = m_wifi.addNetwork(wifiConfiguration);
                Log.d(TAG, "connectToWifiAP [OPEN] add Network returned " + networkId);
                break;
            case WEP:
                wifiConfiguration.SSID = "\"" + ssid + "\"";
                // check the validity of a WEP password
                Pair<Boolean, Boolean> wepCheckResult = checkWEPPassword(password);
                if (!wepCheckResult.first) {
                    Log.i(TAG, "connectToWifiAP  auth type = WEP: password " + password + " invalid length or charecters");
                    return false;
                }
                Log.i(TAG, "connectToWifiAP [WEP] using " + (!wepCheckResult.second ? "ASCII" : "HEX"));
                if (!wepCheckResult.second) {
                    wifiConfiguration.wepKeys[0] = "\"" + password + "\"";
                } else {
                    wifiConfiguration.wepKeys[0] = password;
                }
                wifiConfiguration.priority = 40;
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfiguration.wepTxKeyIndex = 0;
                networkId = m_wifi.addNetwork(wifiConfiguration);
                break;

            case WPA_AUTO:
            case WPA_CCMP:
            case WPA_TKIP:
            case WPA2_AUTO:
            case WPA2_CCMP:
            case WPA2_TKIP: {
                wifiConfiguration.SSID = "\"" + ssid + "\"";
                // handle special case when WPA/WPA2 and 64 length password that can
                // be HEX
                if (password.length() == 64 && password.matches(WEP_HEX_PATTERN)) {
                    wifiConfiguration.preSharedKey = password;
                } else {
                    wifiConfiguration.preSharedKey = "\"" + password + "\"";
                }
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                networkId = m_wifi.addNetwork(wifiConfiguration);
                break;
            }
            default:
                networkId = -1;
                break;
        }
        if (networkId < 0) {
            return false;
        }
        connect(wifiConfiguration, networkId, 30*1000);
        return true;
    }

    public Pair<Boolean, Boolean> checkWEPPassword(String password) {
        Log.d(TAG, "checkWEPPassword");

        if (password == null || password.isEmpty()) {
            Log.w(TAG, "checkWEPPassword empty password");
            return new Pair<Boolean, Boolean>(false, false);
        }

        int length = password.length();
        switch (length) {
            // valid ASCII keys length
            case 5:
            case 13:
            case 16:
            case 29:
                Log.d(TAG, "checkWEPPassword valid WEP ASCII password");
                return new Pair<Boolean, Boolean>(true, false);
            // valid hex keys length
            case 10:
            case 26:
            case 32:
            case 58:
                if (password.matches(WEP_HEX_PATTERN)) {
                    Log.d(TAG, "checkWEPPassword valid WEP password length, and HEX pattern match");
                    return new Pair<Boolean, Boolean>(true, true);
                }
                Log.w(TAG, "checkWEPPassword valid WEP password length, but HEX pattern matching failed: " + WEP_HEX_PATTERN);
                return new Pair<Boolean, Boolean>(false, false);
            default:
                Log.w(TAG, "checkWEPPassword invalid WEP password length: " + length);
                return new Pair<Boolean, Boolean>(false, false);
        }
    }

    public String toHexadecimalString(String pass) {
        byte[] data;

        try {
            data = pass.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            data = pass.getBytes();
        }
        StringBuilder r = new StringBuilder(data.length*2);
        for ( byte b : data) {
            r.append(HEX_CODE[(b >> 4) & 0xF]);
            r.append(HEX_CODE[(b & 0xF)]);
        }
        return r.toString();
    }
}
