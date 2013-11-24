package com.smsbackground;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.ListView;

public class MainActivity extends Activity {

    private SharedPreferences preferences;
    private BluetoothAdapter blueToothAdaptor;
    private final String tag = "Bluetooth";
    private final List<ScannableDevice> bluetoothDevices = new ArrayList<ScannableDevice>();
    private ArrayAdapterItem adapter;
    private ProgressDialog progressScanDialog;
    private Handler blueToothScanHandler;
    private boolean toFurtherScan;

    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(tag, "Device found " + device.getName());

                ScannableDevice presentDevice = null;
                for (ScannableDevice alreadyDiscoveredDevice : bluetoothDevices) {
                    if (device.getAddress().equals(
                            alreadyDiscoveredDevice.getAddress())) {
                        presentDevice = alreadyDiscoveredDevice;
                        break;
                    }
                }

                if (presentDevice == null) {
                    Log.d(tag,
                            "New device discovered .. Adding "
                                    + device.getName());
                    presentDevice = new ScannableDevice();
                    presentDevice.setAddress(device.getAddress());
                    presentDevice.setScanAllowed(Boolean.TRUE);
                    bluetoothDevices.add(presentDevice);
                }
                else {
                    Log.d(tag, "This device is already in the list .. ");
                }

                presentDevice
                        .setName(device.getName() == null ? "<no_name_configured>"
                                : device.getName());
                presentDevice.setLastScannedTime(System.currentTimeMillis());
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(tag, "Discovery is finished .. ");
                dismissDialog();
                if (toFurtherScan) {
                    runAfter(12000);
                }
                else {
                    removeCallbacks();
                }
            }
            adapter.notifyDataSetChanged();
        }

    };

    Runnable blueToothReceiverTask = new Runnable() {

        @Override
        public void run() {
            if (blueToothAdaptor != null) {
                if (!blueToothAdaptor.isDiscovering()) {
                    dismissDialog();
                    progressScanDialog = new ProgressDialog(MainActivity.this);
                    progressScanDialog
                            .setTitle("Scanning BlueTooth Devices .. ");
                    progressScanDialog.setMessage("Please wait .. ");
                    progressScanDialog.show();

                    startBlueToothDiscovery();
                }
                else {
                    Log.d(tag, "Discovery is going on ... ");
                }
            }
        }
    };

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

            preferences = getPreferences(MODE_PRIVATE);

            adapter = new ArrayAdapterItem(this, R.layout.list_view_row_item,
                    bluetoothDevices, preferences);

            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setItemsCanFocus(Boolean.FALSE);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(adapter);

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(blueToothReceiver, filter);

            if (blueToothScanHandler == null) {
                blueToothScanHandler = new Handler();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(tag, "Resuming discovery");
        toFurtherScan = true;
        super.onResume();
        runAfter(5000);
    }

    @Override
    protected void onPause() {
        Log.d(tag, "Pausing discovery");
        super.onPause();
        toFurtherScan = false;
        cancelBlueToothDiscovery();
    }

    @Override
    protected void onDestroy() {
        Log.d(tag, "Cancel discovery");
        super.onDestroy();
        toFurtherScan = false;
        cancelBlueToothDiscovery();
    }

    private void startBlueToothDiscovery() {
        blueToothAdaptor.startDiscovery();
    }

    private void cancelBlueToothDiscovery() {
        blueToothAdaptor.cancelDiscovery();
    }

    private void removeCallbacks() {
        blueToothScanHandler.removeCallbacks(blueToothReceiverTask);
    }

    private void dismissDialog() {
        if (progressScanDialog != null && progressScanDialog.isShowing()) {
            progressScanDialog.dismiss();
        }
    }

    private boolean runAfter(long runEveryMilliseconds) {
        return blueToothScanHandler.postDelayed(blueToothReceiverTask,
                runEveryMilliseconds);
    }

}
