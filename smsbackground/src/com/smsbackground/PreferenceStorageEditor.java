package com.smsbackground;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.gson.Gson;

public class PreferenceStorageEditor implements StorageEditor {

    private static final String SMS_APP_ALLOWED = "sms.app.allowed";
    private static final String BLUETOOTH_DISCOVERY_ALLOWED = "bluetooth.discovery.allowed";
    private static final String DEVICES_SELECTED = "device.selected.set";

    private final SharedPreferences preferences;

    public PreferenceStorageEditor(SharedPreferences preferences) {
        super();
        this.preferences = preferences;
    }

    @Override
    public Set<String> getSelectedDevices() {
        return preferences
                .getStringSet(DEVICES_SELECTED, new HashSet<String>());
    }

    @Override
    public void updateLastScannedTime(ScannableDevice device) {
        Editor editor = preferences.edit();

        Set<String> copyOfSelectedDevices = new HashSet<String>();

        Gson gson = new Gson();

        Set<String> selectedDevices = getSelectedDevices();
        for (String selectedDevice : selectedDevices) {
            ScannableDevice iteratorScannableDevice = gson.fromJson(
                    selectedDevice, ScannableDevice.class);

            if (iteratorScannableDevice.getAddress()
                    .equals(device.getAddress())) {
                iteratorScannableDevice.setLastScannedTime(device
                        .getLastScannedTime());
                selectedDevice = gson.toJson(iteratorScannableDevice,
                        ScannableDevice.class);
            }

            String copyOfSelectedDevice = new String(selectedDevice);
            copyOfSelectedDevices.add(copyOfSelectedDevice);
        }

        editor.putStringSet(DEVICES_SELECTED, copyOfSelectedDevices).commit();
    }

    @Override
    public int updateDeviceStateInStorage(boolean isChecked,
            ScannableDevice device) {
        Editor editor = preferences.edit();

        Set<String> selectedDevices = getSelectedDevices();
        int maxStored = selectedDevices.isEmpty() ? 0 : selectedDevices.size();

        Set<String> copyOfSelectedDevices = new HashSet<String>();
        for (String selectedDevice : selectedDevices) {
            String copyOfSelectedDevice = new String(selectedDevice);
            copyOfSelectedDevices.add(copyOfSelectedDevice);
        }

        boolean found = isDevicePresent(device);

        Gson gson = new Gson();

        if (isChecked && !found) {
            String deviceJson = gson.toJson(device, ScannableDevice.class);
            copyOfSelectedDevices.add(deviceJson);
        }
        else if (!isChecked && found) {
            Iterator<String> iterator = copyOfSelectedDevices.iterator();
            while (iterator.hasNext()) {
                String iteratorJson = iterator.next();
                ScannableDevice iteratorDevice = gson.fromJson(iteratorJson,
                        ScannableDevice.class);

                if (iteratorDevice.getAddress().equals(device.getAddress())) {
                    iterator.remove();
                    break;
                }
            }
        }

        Log.d("Bluetooth", "Selected Devices Old Actual Size = " + maxStored
                + " New Actual Size = " + copyOfSelectedDevices.size());

        editor.putStringSet(DEVICES_SELECTED, copyOfSelectedDevices).commit();

        return copyOfSelectedDevices.size();
    }

    @Override
    public boolean isDevicePresent(ScannableDevice device) {
        boolean found = false;

        Gson gson = new Gson();
        for (String deviceSelectedData : getSelectedDevices()) {
            ScannableDevice comparedDevice = gson.fromJson(deviceSelectedData,
                    ScannableDevice.class);
            if (comparedDevice.getAddress().equals(device.getAddress())) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public void setBoolean(String key, boolean allowed) {
        preferences.edit().putBoolean(key, allowed).commit();
    }

    @Override
    public boolean isDiscoveryScanAllowed() {
        return preferences.getBoolean(BLUETOOTH_DISCOVERY_ALLOWED, false);
    }

    @Override
    public boolean isSmsAllowed() {
        return preferences.getBoolean(SMS_APP_ALLOWED, false);
    }

}
