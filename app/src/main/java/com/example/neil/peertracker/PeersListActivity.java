package com.example.neil.peertracker;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.OnCreateContextMenuListener;


public class PeersListActivity extends ListActivity implements OnCreateContextMenuListener {
    protected static final String TAG = PeerTracker.TAG;
    private PeerTracker m_application;
    private BroadcastReceiver m_receiver;
    private PeerAdapter m_devicesAdapter;
    private Button m_AJConnect;
    private TextView m_currentNetwork;

    private void showAnnounce(Peer device) {
        AlertDialog.Builder alert = new AlertDialog.Builder(PeersListActivity.this);
        alert.setTitle("Announcement of "+device.busName);
        String msg = device.getAnnounce();
        alert.setMessage(msg);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void allJoynConnect() {
        AlertDialog.Builder alert = new AlertDialog.Builder(PeersListActivity.this);
        alert.setTitle("Set channel name");

        final EditText input = new EditText(PeersListActivity.this);
        input.setText("org.alljoyn.BusNode.PeerTracker");
        alert.setView(input);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        m_application.setRealmName(input.getText().toString());
                        m_AJConnect.setText(R.string.AllJoynDisconnect);
                        m_application.doConnect();
                        m_application.makeToast("Connection complete");
                    }
                });
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        alert.show();
    }

    private void allJoynDisconnect(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Are you sure you want to disconnect from AllJoyn?");
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int whichButton) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        m_AJConnect.setText(R.string.AllJoynConnect);
                        m_devicesAdapter.clear();
                        setListAdapter(m_devicesAdapter);
                        m_application.doDisconnect();
                        m_application.makeToast("AJ disconnect done");
                    }
                });
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();

    }

    public void AJPress(View view) {
        m_AJConnect = (Button) view.findViewById(R.id.AllJoynConnect);
        if(m_AJConnect.getText().equals(getString(R.string.AllJoynConnect))){
            allJoynConnect();
        }
        else if(m_AJConnect.getText().equals(getString(R.string.AllJoynDisconnect))){
            allJoynDisconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peers_list);
        m_application = (PeerTracker)getApplication();
        m_currentNetwork = (TextView) findViewById(R.id.current_network_name);

        String ssid = ((PeerTracker)getApplication()).getNetworkManager().getCurrentNetworkSSID();
        m_currentNetwork.setText(getString(R.string.current_network, ssid));

        m_devicesAdapter = new PeerAdapter(this, R.layout.peer_property);
        m_devicesAdapter.setLayoutInflator(getLayoutInflater());
        setListAdapter(m_devicesAdapter);
        registerForContextMenu(getListView());

        m_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Keys.Actions.ACTION_DEVICE_FOUND.equals(intent.getAction())){
                    String appId = intent.getExtras().getString(Keys.Extras.EXTRA_DEVICE_ID);
                    Peer device = m_application.getDevice(appId);
                    m_devicesAdapter.addDevice(device);
                    setListAdapter(m_devicesAdapter);
                }
                else if(Keys.Actions.ACTION_DEVICE_LOST.equals(intent.getAction())){
                    String busName = intent.getExtras().getString(Keys.Extras.EXTRA_BUS_NAME);
                    m_devicesAdapter.removeByBusName(busName);
                    setListAdapter(m_devicesAdapter);
                }
                else if(Keys.Actions.ACTION_CONNECTED_TO_NETWORK.equals(intent.getAction())){
                    String ssid = intent.getStringExtra(Keys.Extras.EXTRA_NETWORK_SSID);
                    m_currentNetwork.setText(getString(R.string.current_network, ssid));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Keys.Actions.ACTION_DEVICE_FOUND);
        filter.addAction(Keys.Actions.ACTION_DEVICE_LOST);
        filter.addAction(Keys.Actions.ACTION_CONNECTED_TO_NETWORK);
        registerReceiver(m_receiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_peers_list, menu);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_application.doDisconnect();
        m_application.makeToast("AJ disconnect done");
        unregisterReceiver(m_receiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Peer device = (Peer) getListAdapter().getItem(info.position);

        if(item.getItemId() == R.id.context_menu_announce){
            showAnnounce(device);
        }
        else if(item.getItemId() == R.id.context_menu_onboarding){
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.putExtra(Keys.Extras.EXTRA_DEVICE_ID, device.uuid);
            startActivity(intent);
        }
        return true;

    }
    //====================================================================
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {// :-)

        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_peers_list, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ListAdapter adapter = getListAdapter();
        Peer item = (Peer) adapter.getItem(info.position);

        if(!item.supportOnboarding)
            menu.removeItem(R.id.context_menu_onboarding);
    }

    //====================================================================
	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
    @Override
    public void onListItemClick(ListView lv, View view, int position, long id) {
        Toast.makeText(getBaseContext(), R.string.device_list_item_click_toast, Toast.LENGTH_SHORT).show();
        super.onListItemClick(lv, view, position, id);
    }
}
