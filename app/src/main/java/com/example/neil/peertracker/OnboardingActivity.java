package com.example.neil.peertracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.alljoyn.onboarding.OnboardingService;
import org.alljoyn.onboarding.transport.MyScanResult;
import org.alljoyn.onboarding.transport.OBLastError;
import org.alljoyn.onboarding.transport.ScanInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class OnboardingActivity extends Activity {
    //General
    protected static final String TAG = PeerTracker.TAG;
    private PeerTracker m_application;
    private BroadcastReceiver m_receiver;
    private Peer m_device;
    private Timer m_timer;
    private int m_tasksToPerform = 0;
    private ProgressDialog m_loadingPopup;
    private AlertDialog m_passwordAlertDialog;

    //Current network
    private TextView m_currentNetwork;

    //Version and other properties
    private TextView m_onbaordingVersion;
    private TextView m_lastErrorCodeValue;
    private TextView m_lastErrorMsgValue;
    private TextView m_stateValue;

    //Scan info
    private Spinner m_scanInfoData;
    private ArrayAdapter<MyScanResult> m_scanInfoAdapter;
    private TextView m_scanInfoAge;
    private AdapterView.OnItemSelectedListener m_scanInfoListener;

    //Network items
    private String m_networkName;
    private String m_networkPassword;
    private short m_networkAuthType;
    private EditText m_networkNameEditText;
    private EditText m_networkPasswordEditText;
    private Spinner m_authTypeSpinner;
    private ArrayAdapter<OnboardingService.AuthType> m_authTypeAdapter;
    private Button m_configureButton;
    private Button m_connectButton;

    private void startOnboardingSession() {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "startSession: onPreExecute");
            }

            @Override
            protected Void doInBackground(Void... params) {
                m_application.startSession(m_device);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.d(TAG, "startSession: onPostExecute");
            }
        };
        task.execute();
    }

    private void getVersion() {
        final AsyncTask<Void, Void, Short> task = new AsyncTask<Void, Void, Short>(){

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "getOnboardingVersion: onPreExecute");
                showLoadingPopup("getting onboarding version");
            }

            @Override
            protected Short doInBackground(Void... params){
                return m_application.getOnboardingVersion();
            }

            @Override
            protected void onPostExecute(Short result){
                short version = result.shortValue();
                m_onbaordingVersion.setText(String.valueOf(version));
                Log.d(TAG, "getOnboardingVersion: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        task.execute();
    }

    private void getLastError() {

        final AsyncTask<Void, Void, OBLastError> task = new AsyncTask<Void, Void, OBLastError>(){

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "getLastError: onPreExecute");
                showLoadingPopup("getting last error version");
            }

            @Override
            protected OBLastError doInBackground(Void... params){
                return m_application.getLastError();
            }

            @Override
            protected void onPostExecute(OBLastError result){

                m_application.makeToast("get last error done");
                String msg = result.getErrorMessage();
                m_lastErrorCodeValue.setText(result.getErrorCode()+"");
                m_lastErrorMsgValue.setText("".equals(msg) ? "No error msg" : msg);
                Log.d(TAG, "getLastError: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        task.execute();
    }

    private void getState() {

        final AsyncTask<Void, Void, Short> task = new AsyncTask<Void, Void, Short>(){

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "getState: onPreExecute");
                showLoadingPopup("getting state");
            }

            @Override
            protected Short doInBackground(Void... params){
                return m_application.getState();
            }

            @Override
            protected void onPostExecute(Short result){

                m_application.makeToast("get state done");
                short version = result.shortValue();
                m_stateValue.setText(String.valueOf(version));
                Log.d(TAG, "getState: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        task.execute();
    }

    private void getScanInfo() {
        final AsyncTask<Void, Void, ScanInfo> task = new AsyncTask<Void, Void, ScanInfo>(){

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "getScanInfo: onPreExecute");
                showLoadingPopup("getting scan info");
            }

            @Override
            protected ScanInfo doInBackground(Void... params){
                return m_application.getScanInfo();
            }

            @Override
            protected void onPostExecute(ScanInfo scan){

                m_application.makeToast("get scan info done");
                //Display the given scan result. if there are no scan result
                //we display "no results" to the user.
                if(scan != null && scan.getScanResults() != null){

                    MyScanResult[] scanInfo = scan.getScanResults();
                    m_scanInfoAdapter.clear();
                    int age = scan.m_age;
                    m_scanInfoAge.setText(age+"");

                    for (MyScanResult scanResult : scanInfo) {
                        if(!scanResult.m_ssid.startsWith("AJ_")){
                            m_scanInfoAdapter.add(new MyScanResultWrapper(scanResult));
                        }
                    }
                    if(m_scanInfoAdapter.getCount() == 0){
                        MyScanResult sr = new MyScanResult();
                        sr.m_authType = 0;
                        sr.m_ssid = "No results";
                        m_scanInfoAdapter.add(new MyScanResultWrapper(sr));
                        m_scanInfoData.setEnabled(false);
                    }
                    m_scanInfoData.setAdapter(m_scanInfoAdapter);
                }
                else{
                    //No scan info results
                    MyScanResult sr = new MyScanResult();
                    sr.m_authType = 0;
                    sr.m_ssid = "No results";
                    m_scanInfoAdapter.add(new MyScanResultWrapper(sr));
                    m_scanInfoData.setAdapter(m_scanInfoAdapter);

                    m_scanInfoAge.setText("No results");
                }
                Log.d(TAG, "getScanInfo: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        task.execute();
    }

    private void configure(){
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){

            private String finalPassword;

            @Override
            protected void onPreExecute() {

                //Take the user parameters from the UI:
                m_networkName = m_networkNameEditText.getText().toString();
                m_networkPassword = m_networkPasswordEditText.getText().toString();
                OnboardingService.AuthType selectedAuthType = (OnboardingService.AuthType) m_authTypeSpinner.getSelectedItem();
                m_networkAuthType = selectedAuthType.getTypeId();

                //In case password is WEP and its format is HEX - leave it in HEX format.
                //otherwise convert it from ASCII to HEX
                finalPassword = m_networkPassword;
                if(OnboardingService.AuthType.WEP.equals(selectedAuthType)){

                    Pair<Boolean, Boolean> wepCheckResult = m_application.getNetworkManager().checkWEPPassword(finalPassword);
                    if (!wepCheckResult.first) {//Invalid WEP password
                        Log.i(TAG, "Auth type = WEP: password " + finalPassword + " invalid length or charecters");

                    }
                    else{
                        Log.i(TAG, "configure wifi [WEP] using " + (!wepCheckResult.second ? "ASCII" : "HEX"));
                        if (!wepCheckResult.second) {//ASCII. Convert it to HEX
                            finalPassword = m_application.getNetworkManager().toHexadecimalString(finalPassword);
                        }
                    }
                }
                else{//Other auth type than WEP -> convert password to HEX
                    finalPassword = m_application.getNetworkManager().toHexadecimalString(finalPassword);
                }

                m_connectButton.setEnabled(true);
                Log.d(TAG, "configure: onPreExecute");
                showLoadingPopup("configuring network");
            }

            @Override
            protected Void doInBackground(Void... params){

                m_application.configureNetwork(m_networkName, finalPassword, m_networkAuthType);
                return null;
            }

            @Override
            protected void onPostExecute(Void result){
                m_application.makeToast("Configure network done");
                Log.d(TAG, "configure: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        m_tasksToPerform = 1;
        task.execute();
    }

    private void connect(){
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "connect: onPreExecute");
                showLoadingPopup("connect network");
            }

            @Override
            protected Void doInBackground(Void... params){
                m_application.connectNetwork();
                return null;
            }

            @Override
            protected void onPostExecute(Void result){
                Log.d(TAG, "connect: onPostExecute");
                m_application.makeToast("Connect network done");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        m_application.getNetworkManager().connectToAP(m_networkName, m_networkPassword, (short)m_networkAuthType);
                        m_tasksToPerform--;
                        dismissLoadingPopup();
                    }
                }, 3*1000);
            }
        };
        m_tasksToPerform = 1;
        task.execute();
    }

    private void offboard(){
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){

            @Override
            protected void onPreExecute() {

                Log.d(TAG, "offboard: onPreExecute");
                showLoadingPopup("offboarding");
            }

            @Override
            protected Void doInBackground(Void... params){
                m_application.offboard();
                return null;
            }

            @Override
            protected void onPostExecute(Void result){
                m_application.makeToast("Offboard done");
                Log.d(TAG, "offboard: onPostExecute");
                m_tasksToPerform--;
                dismissLoadingPopup();
            }
        };
        m_tasksToPerform = 1;
        task.execute();
    }

    private void closeScreen() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Error");
        alert.setMessage("Device was not found");

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                finish();
            }
        });
        alert.show();
    }

    private void initPasswordAlertDialog() {

        AlertDialog.Builder alert = new AlertDialog.Builder(OnboardingActivity.this);
        alert.setTitle("Your password is incorrect. Please enter the correct one");
        alert.setCancelable(false);

        final EditText input = new EditText(OnboardingActivity.this);
        input.setText("");
        alert.setView(input);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String devicePassword = input.getText().toString();
                m_device.password = devicePassword.toCharArray();//Update the device password
                dialog.dismiss();
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        m_passwordAlertDialog = alert.create();
    }

    private void showLoadingPopup(String msg) {
        if (m_loadingPopup !=null){
            if(!m_loadingPopup.isShowing()){
                m_loadingPopup = ProgressDialog.show(this, "", msg, true);
                Log.d(TAG, "showLoadingPopup with msg = "+msg);
            }
            else{
                m_loadingPopup.setMessage(msg);
                Log.d(TAG, "setMessage with msg = "+msg);
            }
        }
        m_timer = new Timer();
        m_timer.schedule(new TimerTask() {
            public void run() {
                if (m_loadingPopup !=null && m_loadingPopup.isShowing()){
                    Log.d(TAG, "showLoadingPopup dismissed the popup");
                    m_loadingPopup.dismiss();
                };
            }

        },30*1000);
    }

    private void dismissLoadingPopup() {
        if(m_tasksToPerform == 0){
            if (m_loadingPopup != null){
                Log.d(TAG, "dismissLoadingPopup dismissed the popup");
                m_loadingPopup.dismiss();
                if(m_timer != null){
                    m_timer.cancel();
                }
            }
        }
    }

    private class ScanInfoListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

            //Set the selected ssid to be the network name
            MyScanResult item = (MyScanResult)m_scanInfoData.getSelectedItem();
            m_networkNameEditText.setText(item.m_ssid);

            //Set the selected authType (given from the selected scan result)
            OnboardingService.AuthType authType = OnboardingService.AuthType.getAuthTypeById(item.m_authType);
            if(authType == null){
                authType = OnboardingService.AuthType.ANY;
            }

            // Search the authType in the list and make it the first selection.
            int authTypePosition = 0;
            OnboardingService.AuthType[] values = OnboardingService.AuthType.values();
            for (int i = 0; i < values.length; i++){
                if (values[i].equals(authType)){
                    authTypePosition = i;
                    break;
                }
            }
            m_authTypeSpinner.setSelection(authTypePosition);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private class MyScanResultWrapper extends MyScanResult{

        public MyScanResultWrapper(MyScanResult scanResult) {
            super();
            this.m_ssid = scanResult.m_ssid;
            this.m_authType = scanResult.m_authType;
        }
        @Override
        public String toString() {
            return this.m_ssid;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //General
        setContentView(R.layout.activity_onboarding);
        String deviceId = getIntent().getStringExtra(Keys.Extras.EXTRA_DEVICE_ID);
        m_application = (PeerTracker)getApplication();
        m_device = m_application.getDevice(deviceId);
        if(m_device == null){
            closeScreen();
            return;
        }

        startOnboardingSession();//Start the onboarding client and crate a session with it.

        m_loadingPopup = new ProgressDialog(this);

        //Current Network
        m_currentNetwork = (TextView) findViewById(R.id.current_network_name);
        String ssid = m_application.getNetworkManager().getCurrentNetworkSSID();
        m_currentNetwork.setText(getString(R.string.current_network, ssid));

        //Version and other properties
        m_onbaordingVersion = (TextView)findViewById(R.id.onboarding_version_value);
        m_lastErrorCodeValue = (TextView)findViewById(R.id.last_error_code_value);
        m_lastErrorMsgValue = (TextView)findViewById(R.id.last_error_msg_value);
        m_stateValue = (TextView)findViewById(R.id.state_value);

        //Scan info
        m_scanInfoAdapter = new ArrayAdapter<MyScanResult>(OnboardingActivity.this, android.R.layout.simple_spinner_item);
        m_scanInfoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_scanInfoData = (Spinner) findViewById(R.id.scan_info_data_value);
        m_scanInfoAge = (TextView) findViewById(R.id.scan_info_age_value);

        //Network elements
        m_networkNameEditText = (EditText) findViewById(R.id.network_name);
        m_networkPasswordEditText = (EditText)findViewById(R.id.network_password);
        m_authTypeSpinner = (Spinner) findViewById(R.id.auth_type);

        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(m_networkNameEditText.getText().length() == 0)
                    m_configureButton.setEnabled(false);
                else
                    m_configureButton.setEnabled(true);
            }
        };
        m_networkNameEditText.addTextChangedListener(textWatcher);

        m_configureButton = (Button)findViewById(R.id.configure_button);
        m_connectButton = (Button)findViewById(R.id.connect_button);
        m_connectButton.setEnabled(false);

        initPasswordAlertDialog();

        //************************** Version, LastError, State, ScanInfo **************************
        m_tasksToPerform = 4;
        getVersion();
        getLastError();
        getState();
        getScanInfo();

        //************************** Get Scan Info **************************
        m_scanInfoListener = new ScanInfoListener();
        m_scanInfoData.setOnItemSelectedListener(m_scanInfoListener);

        //************************** AuthType Spinner **************************
        m_authTypeAdapter = new ArrayAdapter<OnboardingService.AuthType>(this, R.layout.spinner_item_white);
        m_authTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        OnboardingService.AuthType[] authTypes = OnboardingService.AuthType.values();
        List<OnboardingService.AuthType> temp = Arrays.asList(authTypes);
        m_authTypeAdapter.addAll(temp);
        m_authTypeSpinner.setAdapter(m_authTypeAdapter);

        //************************** Buttons **************************
        m_configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configure();
            }
        });

        m_connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        //************************** receiver **************************
        //This receiver get notified when a new alljoyn device is found or lost,
        //add when the network state has changes (connected or disconnected, connecting, etc.)
        m_receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if(Keys.Actions.ACTION_PASSWORD_IS_INCORRECT.equals(intent.getAction())){
                    if(m_passwordAlertDialog != null && !m_passwordAlertDialog.isShowing())
                        m_passwordAlertDialog.show();
                }
                else if(Keys.Actions.ACTION_ERROR.equals(intent.getAction())){
                    String error = intent.getStringExtra(Keys.Extras.EXTRA_ERROR);
                    m_application.showAlert(OnboardingActivity.this, error);
                }
                else if(Keys.Actions.ACTION_CONNECTED_TO_NETWORK.equals(intent.getAction())){

                    String ssid = intent.getStringExtra(Keys.Extras.EXTRA_NETWORK_SSID);
                    m_currentNetwork.setText(getString(R.string.current_network, ssid));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Keys.Actions.ACTION_PASSWORD_IS_INCORRECT);
        filter.addAction(Keys.Actions.ACTION_ERROR);
        filter.addAction(Keys.Actions.ACTION_CONNECTED_TO_NETWORK);
        registerReceiver(m_receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_application.endSession();
        if(m_receiver != null){
            try{
                unregisterReceiver(m_receiver);
            } catch (IllegalArgumentException e) {}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_onboarding, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_onboarding_refresh:
                m_tasksToPerform = 4;
                m_scanInfoData.setOnItemSelectedListener(new ScanInfoListener());
                getVersion();
                getLastError();
                getState();
                getScanInfo();
                break;
            case R.id.menu_offboard:
                offboard();
                break;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
