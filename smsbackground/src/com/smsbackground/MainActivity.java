package com.smsbackground;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.Gson;

public class MainActivity extends Activity {

    private ArrayAdapterItem adapter;
    private TextView statusCheckedView;
    private Switch discoverySwitch;
    private Switch actionBarMainSwitch;

    private SharedPreferences preferences;

    private BluetoothAdapter blueToothAdaptor;

    private Handler blueToothScanHandler;
    private Handler actionBarInitializationHandler;

    private SmsMessageBroadcastReceiver smsMessageBroadcastReceiver;

    private StorageEditor storageEditor;

    private final List<ScannableDevice> bluetoothDevices = new ArrayList<ScannableDevice>();

    private final String tag = "Bluetooth";
    private boolean allowNewDiscovery = false;
    private boolean allowSms = false;

    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(tag, "Device found " + device.getName());

                ScannableDevice presentDevice = null;
                synchronized (bluetoothDevices) {
                    for (ScannableDevice alreadyDiscoveredDevice : bluetoothDevices) {
                        if (device.getAddress().equals(
                                alreadyDiscoveredDevice.getAddress())) {
                            presentDevice = alreadyDiscoveredDevice;
                            break;
                        }
                    }

                    if (presentDevice == null) {
                        Log.d(tag, "New device discovered " + device.getName());
                        presentDevice = new ScannableDevice();
                        presentDevice.setAddress(device.getAddress());
                        presentDevice.setScanAllowed(Boolean.TRUE);
                        if (allowNewDiscovery) {
                            bluetoothDevices.add(presentDevice);
                        }
                    }
                    else {
                        Log.d(tag, "This device is already in the list .. ");
                    }
                }

                presentDevice
                        .setName(device.getName() == null ? "<no_name_configured>"
                                : device.getName());
                presentDevice.setLastScannedTime(System.currentTimeMillis());

                storageEditor.updateLastScannedTime(presentDevice);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(tag, "Discovery is finished .. ");
                performVisibilityChecksBeforeMakingListViewVisible();
                runBlueToothDiscoveryAfter(10000);
            }
            adapter.notifyDataSetChanged();
        }

    };

    Runnable blueToothReceiverTask = new Runnable() {

        @Override
        public void run() {
            if (blueToothAdaptor != null) {
                if (!blueToothAdaptor.isDiscovering()) {
                    startBlueToothDiscovery();
                }
                else {
                    Log.d(tag, "Discovery is going on ... ");
                }
            }
        }
    };

    Runnable actionBarAndOtherInitialization = new Runnable() {

        @Override
        public void run() {
            actionBarMainSwitch = (Switch) findViewById(R.id.action_bar_switch);
            actionBarMainSwitch
                    .setOnCheckedChangeListener(actionBarMainSwitchListener);
            actionBarMainSwitch.setChecked(allowSms);

            performVisibilityChecksBeforeMakingListViewVisible();
            adapter.notifyDataSetChanged();
        }
    };

    OnCheckedChangeListener discoverySwitchListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            allowNewDiscovery = isChecked;
            doCheckForBlueToothAndSetInPreferencesIfAllowed(isChecked,
                    "bluetooth.discovery.allowed", buttonView);
            if (isChecked) {
                runBlueToothDiscoveryAfter(1000);
            }
        }
    };

    OnCheckedChangeListener actionBarMainSwitchListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            doCheckForBlueToothAndSetInPreferencesIfAllowed(isChecked,
                    "sms.app.allowed", buttonView);
            if (isChecked) {
                smsMessageBroadcastReceiver.register();
            }
            else {
                smsMessageBroadcastReceiver.unRegister();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        runActionBarInitializationAfter(4000);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            startActivityForResult(new Intent(
                    android.provider.Settings.ACTION_SETTINGS), 0);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blueToothAdaptor = BluetoothAdapter.getDefaultAdapter();
        if (blueToothAdaptor == null) {
            Log.d(tag, "Device does not support bluetooth");
        }
        else {
            if (blueToothAdaptor.isEnabled()) {
                Log.d(tag, "Bluetooth is enabled");
            }
            else {
                Log.d(tag, "Bluetooth is disabled");
            }

            statusCheckedView = (TextView) findViewById(R.id.selectedItemsStatusView);

            preferences = getPreferences(MODE_PRIVATE);
            storageEditor = new PreferenceStorageEditor(preferences);

            adapter = new ArrayAdapterItem(this, R.layout.list_view_row_item,
                    bluetoothDevices, statusCheckedView, storageEditor);

            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setItemsCanFocus(Boolean.FALSE);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(adapter);

            discoverySwitch = (Switch) findViewById(R.id.discovery_monitored_switch);
            discoverySwitch.setOnCheckedChangeListener(discoverySwitchListener);

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(blueToothReceiver, filter);

            smsMessageBroadcastReceiver = new SmsMessageBroadcastReceiver(
                    getApplicationContext(), preferences);

            if (blueToothScanHandler == null) {
                blueToothScanHandler = new Handler();
            }

            if (actionBarInitializationHandler == null) {
                actionBarInitializationHandler = new Handler();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(tag, "Resuming discovery");
        super.onResume();

        cancelDiscovery();

        loadUpPreferences();

        if (allowSms) {
            runBlueToothDiscoveryAfter(10000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelDiscovery();
        smsMessageBroadcastReceiver.unRegister();
        unregisterReceiver(blueToothReceiver);
    }

    private void cancelDiscovery() {
        blueToothAdaptor.cancelDiscovery();
    }

    private void loadUpPreferences() {
        allowNewDiscovery = storageEditor.isDiscoveryScanAllowed();
        discoverySwitch.setChecked(allowNewDiscovery);

        allowSms = storageEditor.isSmsAllowed();

        Set<String> selectedDevices = storageEditor.getSelectedDevices();

        synchronized (bluetoothDevices) {
            Gson gson = new Gson();
            for (String deviceSelectedData : selectedDevices) {
                ScannableDevice device = gson.fromJson(deviceSelectedData,
                        ScannableDevice.class);
                boolean isDiscoveredInThisIteration = false;
                for (ScannableDevice currentDevice : bluetoothDevices) {
                    if (currentDevice.getAddress().equals(device.getAddress())) {
                        isDiscoveredInThisIteration = true;
                        break;
                    }
                }
                if (!isDiscoveredInThisIteration) {
                    bluetoothDevices.add(device);
                }
            }
        }
    }

    private void startBlueToothDiscovery() {
        blueToothAdaptor.startDiscovery();
    }

    private boolean runBlueToothDiscoveryAfter(long runEveryMilliseconds) {
        return blueToothScanHandler.postDelayed(blueToothReceiverTask,
                runEveryMilliseconds);
    }

    private boolean runActionBarInitializationAfter(long runAfterMilliseconds) {
        return actionBarInitializationHandler.postDelayed(
                actionBarAndOtherInitialization, runAfterMilliseconds);
    }

    private void doCheckForBlueToothAndSetInPreferencesIfAllowed(
            boolean isChecked, String key, CompoundButton buttonView) {
        if (!blueToothAdaptor.isEnabled() && isChecked) {
            showBlueToothDisabledDialog();
            buttonView.setChecked(false);
        }
        else {
            storageEditor.setBoolean(key, isChecked);
        }
    }

    private void showBlueToothDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("BlueTooth not enabled. Please enable bluetooth!!")
                .setCancelable(false).setPositiveButton("OK", null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void performVisibilityChecksBeforeMakingListViewVisible() {
        if (bluetoothDevices.size() > 0) {
            View selectedItemsStatusView = findViewById(R.id.selectedItemsStatusView);
            selectedItemsStatusView.setVisibility(View.VISIBLE);

            View listView = findViewById(R.id.listView);
            listView.setVisibility(View.VISIBLE);
        }
    }

}
