package com.example.neil.peertracker;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class NetworksListActivity extends ListActivity {

    protected static final String TAG = PeerTracker.TAG;
    private NetworkAdapter m_adapter;
    private Button m_scanNetworkButton;
    private ListView m_list;
    private NetworkManager m_networkManager;
    private ProgressDialog m_progressDialog;
    private ProgressDialog m_loadingPopup;
    private Timer m_timer;
    private BroadcastReceiver m_receiver;

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
                        if (m_progressDialog!=null && m_progressDialog.isShowing()) {
                            m_progressDialog.dismiss();
                        }
                    }
                }, 2000);
            }
            // filter out AP that don't start with "AJ_"
        }, "AJ_");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_networks_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
