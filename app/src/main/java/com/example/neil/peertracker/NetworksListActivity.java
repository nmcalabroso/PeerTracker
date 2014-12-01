package com.example.neil.peertracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.OnCreateContextMenuListener;


public class NetworksListActivity extends ListActivity implements OnCreateContextMenuListener {

    protected static final String TAG = PeerTracker.TAG;
    private NetworkAdapter m_adapter;
    private Button m_scanNetworkButton;
    private ListView m_list;
    private NetworkManager m_networkManager;
    private ProgressDialog m_progressDialog;
    private ProgressDialog m_loadingPopup;
    private Timer m_timer;
    private BroadcastReceiver m_receiver;

    public void scanNetwork(View view) {
        m_progressDialog = ProgressDialog.show(NetworksListActivity.this, "", getString(R.string.network_activity_scanning));
        m_networkManager.scanForNetwork(getApplicationContext(), new WifiManagerListener() {
            public void OnScanResultComplete(final List<ScanResult> results) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // reset the list to show up to date scan result
                        m_adapter.clear();
                        m_adapter.addAll(results);
                        m_list.setAdapter(m_adapter);
                        m_adapter.notifyDataSetChanged();
                    }
                });
                // set a timer for the progress dialog
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    public void run() {
                        if (m_progressDialog != null && m_progressDialog.isShowing()) {
                            m_progressDialog.dismiss();
                        }
                    }
                }, 2000);
            }
            // filter out AP that don't start with "AJ_"
        }, "AJ_");
    }

    private boolean isSsidEquals(String ssid1, String ssid2) {
        if (ssid1 == null || ssid1.length() == 0 || ssid2 == null || ssid2.length() == 0){
            return false;
        }
        ssid1 = ssid1.replace("\"", "");
        ssid2 = ssid2.replace("\"", "");
        return ssid1.equals(ssid2);
    }

    private void showLoadingPopup(String msg) {
        if (m_loadingPopup !=null){
            if(!m_loadingPopup.isShowing()){
                m_loadingPopup = ProgressDialog.show(this, "", msg, true);
            }
            else{
                m_loadingPopup.setMessage(msg);
            }
        }
        m_timer = new Timer();
        m_timer.schedule(new TimerTask() {
            public void run() {
                if (m_loadingPopup !=null && m_loadingPopup.isShowing()){
                    m_loadingPopup.dismiss();
                };
            }

        },5*1000);
    }

    private void goToDeviceListActivity(final String requestedSSID){
        m_receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                    if(networkInfo.getState().equals(NetworkInfo.State.CONNECTED)){
                        WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                        String currentSSID = wifiInfo.getSSID();
                        Log.d(TAG, "check if "+requestedSSID+" equals "+currentSSID);
                        if(isSsidEquals(requestedSSID, currentSSID)){
                            unregisterReceiver(this);
                            Intent next = new Intent(NetworksListActivity.this, PeersListActivity.class);
                            dismissLoadingPopup();
                            startActivity(next);
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(m_receiver, filter);
    }

    private void dismissLoadingPopup() {
        if (m_loadingPopup != null){
            m_loadingPopup.dismiss();
            if(m_timer != null){
                m_timer.cancel();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networks_list);
        m_networkManager = ((PeerTracker) getApplication()).getNetworkManager();
        m_list = (ListView) findViewById(android.R.id.list);
        m_adapter = new NetworkAdapter(this, R.id.network_name_row_textview);
        m_list.setAdapter(m_adapter);
        m_loadingPopup = new ProgressDialog(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_networks_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ScanResult scanItem = m_adapter.getItem(position);
        if (scanItem != null){
            if(!isSsidEquals(scanItem.SSID, m_networkManager.getCurrentNetworkSSID())){
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(R.string.enter_wifi_password_title);
                alert.setMessage(R.string.enter_wifi_password_message);

                View view = getLayoutInflater().inflate(R.layout.password_popup, null);
                final EditText input = (EditText)view.findViewById(R.id.passwordEditText);
                final CheckBox showPassword = (CheckBox)view.findViewById(R.id.showPasswordCheckBox);
                alert.setView(view);

                showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked)
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                        else
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                });

                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        showLoadingPopup(getString(R.string.connecting_to, scanItem.SSID));
                        String pass = input.getText().toString();

                        boolean succeeded = m_networkManager.connectToAP(scanItem.SSID, pass, scanItem.capabilities);
                        if(succeeded){
                            System.out.println("yow");
                            goToDeviceListActivity(scanItem.SSID);
                        }
                        else{
                            dismissLoadingPopup();
                            AlertDialog.Builder alert = new AlertDialog.Builder(NetworksListActivity.this);
                            alert.setTitle(R.string.failed_to_connect_to_wifi_title);
                            alert.setMessage(getString(R.string.failed_to_connect_to_wifi_message,scanItem.SSID));
                            alert.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });
                            alert.show();
                        }
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
            }
            else{
                goToDeviceListActivity(scanItem.SSID);
            }
        }
    }
}
