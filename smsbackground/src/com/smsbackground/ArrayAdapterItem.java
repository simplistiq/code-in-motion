package com.smsbackground;

import java.util.List;

import android.app.Activity;
import android.content.Context;
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

public class ArrayAdapterItem extends ArrayAdapter<ScannableDevice> {

    private final Context context;
    private final int resource;
    private final List<ScannableDevice> objects;
    private final TextView statusCheckedView;
    private final StorageEditor storageEditor;

    public ArrayAdapterItem(Context context, int resource,
            List<ScannableDevice> objects, TextView statusCheckedView,
            StorageEditor storageEditor) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
        this.statusCheckedView = statusCheckedView;
        this.storageEditor = storageEditor;
    }

    private final OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            ScannableDevice device = (ScannableDevice) buttonView.getTag();
            device.setScanAllowed(buttonView.isChecked());

            Log.d("Bluetooth", "checked device " + device.getName()
                    + " checked = " + isChecked);

            int maxStored = storageEditor.updateDeviceStateInStorage(isChecked,
                    device);

            String currentSelectedStatus = String.format(context.getResources()
                    .getString(R.string.number_of_bluetooth_devices_selected),
                    maxStored);

            statusCheckedView.setText(currentSelectedStatus);
            statusCheckedView.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannableDevice device = objects.get(position);

        boolean isDevicePresent = storageEditor.isDevicePresent(device);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
        }

        TextView textViewItem = (TextView) convertView
                .findViewById(R.id.textViewItem);
        textViewItem.setText(device.getName());
        textViewItem.setTag(device.getAddress());

        TextView textViewDetectedTimeItem = (TextView) convertView
                .findViewById(R.id.textViewItemDetectionTimeInterval);
        textViewDetectedTimeItem.setText(DateUtils.getRelativeTimeSpanString(
                device.getLastScannedTime(), System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS));

        CheckBox checkbox = (CheckBox) convertView
                .findViewById(R.id.textViewCheckBox);
        checkbox.setTag(device);
        checkbox.setOnCheckedChangeListener(onCheckedChangeListener);
        checkbox.setChecked(isDevicePresent);

        return convertView;
    }

}
