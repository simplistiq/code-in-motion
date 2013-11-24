package com.smsbackground;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

    public ArrayAdapterItem(Context context, int resource,
            List<ScannableDevice> objects, SharedPreferences preferences) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
        this.preferences = preferences;
    }

    static class ViewHolder {
        protected ScannableDevice device;
        protected CheckBox checkbox;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannableDevice device = objects.get(position);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.device = device;
            viewHolder.checkbox = (CheckBox) convertView
                    .findViewById(R.id.textViewCheckBox);
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
                            Map<String, ?> preferenceData = preferences
                                    .getAll();
                            int maxStored = (preferenceData == null
                                    || preferenceData.isEmpty() ? 0
                                    : preferenceData.size());

                            if (isChecked) {
                                Gson gson = new Gson();
                                String deviceJson = gson.toJson(device);
                                preferences
                                        .edit()
                                        .putString(device.getAddress(),
                                                deviceJson).commit();
                                maxStored++;
                            }
                            else {
                                preferences.edit().remove(device.getAddress())
                                        .commit();
                                maxStored--;
                            }

                            String.format(
                                    context.getResources()
                                            .getString(
                                                    R.string.number_of_bluetooth_devices_selected),
                                    maxStored);
                        }
                    });
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
}
