package com.smsbackground;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.gson.Gson;

public class ArrayAdapterItem extends ArrayAdapter<ScannableDevice> {

    private final Context context;
    private final int resource;
    private final List<ScannableDevice> objects;
    private final SharedPreferences preferences;
    private final TextView statusCheckedView;

    public ArrayAdapterItem(Context context, int resource,
            List<ScannableDevice> objects, SharedPreferences preferences,
            TextView statusCheckedView) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
        this.preferences = preferences;
        this.statusCheckedView = statusCheckedView;
    }

    static class ViewHolder {
        protected ScannableDevice device;
        protected CheckBox checkbox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannableDevice device = objects.get(position);

        boolean isDevicePresent = isDevicePresent(getSelectedDevices(), device);

        CheckBox checkbox = null;
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.device = device;
            checkbox = (CheckBox) convertView
                    .findViewById(R.id.textViewCheckBox);
            viewHolder.checkbox = checkbox;
            viewHolder.checkbox
                    .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                boolean isChecked) {
                            ScannableDevice device = viewHolder.device;
                            device.setScanAllowed(buttonView.isChecked());
                            Log.d("Bluetooth",
                                    "checked device " + device.getName()
                                            + " checked = " + isChecked);

                            int maxStored = updatePreferences(isChecked, device);

                            String currentSelectedStatus = String
                                    .format(context
                                            .getResources()
                                            .getString(
                                                    R.string.number_of_bluetooth_devices_selected),
                                            maxStored);

                            statusCheckedView.setText(currentSelectedStatus);
                            statusCheckedView.setVisibility(View.VISIBLE);
                        }
                    });
            if (isDevicePresent) {
                viewHolder.checkbox.setChecked(isDevicePresent);
                updateLastScannedTime(device);
            }
        }

        TextView textViewItem = (TextView) convertView
                .findViewById(R.id.textViewItem);
        textViewItem.setText(device.getName() != null ? device.getName()
                : "<no_name_bluetooth>");
        textViewItem.setTag(device.getAddress());

        TextView textViewDetectedTimeItem = (TextView) convertView
                .findViewById(R.id.textViewItemDetectionTimeInterval);
        textViewDetectedTimeItem.setText(DateUtils.getRelativeTimeSpanString(
                device.getLastScannedTime(), System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS));

        return convertView;
    }

    private boolean isDevicePresent(Set<String> selectedDevices,
            ScannableDevice device) {
        boolean found = false;

        Gson gson = new Gson();
        for (String deviceSelectedData : selectedDevices) {
            ScannableDevice comparedDevice = gson.fromJson(deviceSelectedData,
                    ScannableDevice.class);
            if (comparedDevice.getAddress().equals(device.getAddress())) {
                found = true;
                break;
            }
        }
        return found;
    }

    private Set<String> getSelectedDevices() {
        return preferences.getStringSet("device.selected.set",
                new HashSet<String>());
    }

    private void updateLastScannedTime(ScannableDevice device) {
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

        editor.putStringSet("device.selected.set", copyOfSelectedDevices)
                .commit();
    }

    private int updatePreferences(boolean isChecked, ScannableDevice device) {
        Editor editor = preferences.edit();

        Set<String> selectedDevices = getSelectedDevices();
        int maxStored = selectedDevices.isEmpty() ? 0 : selectedDevices.size();

        Set<String> copyOfSelectedDevices = new HashSet<String>();
        for (String selectedDevice : selectedDevices) {
            String copyOfSelectedDevice = new String(selectedDevice);
            copyOfSelectedDevices.add(copyOfSelectedDevice);
        }

        boolean found = isDevicePresent(copyOfSelectedDevices, device);

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

        editor.putStringSet("device.selected.set", copyOfSelectedDevices)
                .commit();

        return copyOfSelectedDevices.size();
    }
}
