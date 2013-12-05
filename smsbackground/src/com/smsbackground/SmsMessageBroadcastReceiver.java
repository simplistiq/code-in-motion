package com.smsbackground;

import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.gson.Gson;

public class SmsMessageBroadcastReceiver extends BroadcastReceiver {

    private final SmsManager smsManager;
    private final Context context;
    private final StorageEditor storageEditor;

    public SmsMessageBroadcastReceiver(Context context,
            StorageEditor storageEditor) {
        this.context = context;
        this.storageEditor = storageEditor;
        this.smsManager = SmsManager.getDefault();
    }

    public void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

        context.registerReceiver(this, intentFilter);
    }

    public void unRegister() {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        Log.d("Bluetooth", "Received SMS");

        if (extras != null) {
            Object[] smsExtra = (Object[]) extras.get("pdus");

            Set<String> phoneNumbers = new HashSet<String>();
            for (int i = 0; i < smsExtra.length; i++) {
                SmsMessage smsMessage = SmsMessage
                        .createFromPdu((byte[]) smsExtra[i]);

                String phoneNumber = smsMessage.getDisplayOriginatingAddress();

                Log.d("Bluetooth", "Phone Number received " + phoneNumber);

                phoneNumbers.add(phoneNumber);
            }

            boolean weAreInProximity = findIfInProximity();

            if (weAreInProximity) {
                sendSms(phoneNumbers);
            }
        }
    }

    private void sendSms(Set<String> phoneNumbers) {
        String defaultReply = "I am driving right now";
        for (String phoneNumber : phoneNumbers) {
            Log.d("Bluetooth", "Send default reply for phoneNumber "
                    + phoneNumber);
            smsManager.sendTextMessage(phoneNumber, null, defaultReply, null,
                    null);
        }
    }

    private boolean findIfInProximity() {
        boolean isInProximity = false;

        Set<String> selectedDevices = storageEditor.getSelectedDevices();

        Gson gson = new Gson();
        for (String deviceJson : selectedDevices) {
            ScannableDevice device = gson.fromJson(deviceJson,
                    ScannableDevice.class);
            long currentTimeInMillis = System.currentTimeMillis();
            long elapsedTimeWhenlastSeen = currentTimeInMillis
                    - device.getLastScannedTime().longValue();

            Log.d("Bluetooth", "Current Time = " + currentTimeInMillis
                    + " Last Scanned Device Time = "
                    + device.getLastScannedTime().longValue()
                    + " Difference = " + elapsedTimeWhenlastSeen);

            if (elapsedTimeWhenlastSeen < 120000) {
                isInProximity = true;
                break;
            }
        }

        return isInProximity;
    }
}
