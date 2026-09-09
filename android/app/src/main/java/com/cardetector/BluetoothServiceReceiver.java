package com.cardetector;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device == null) {
            return;
        }

        String deviceName = device.getName() != null ? device.getName() : "Unknown";
        
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Log.d(TAG, "Bluetooth connected: " + deviceName);
            // In a production app, you might want to send a local notification here
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Log.d(TAG, "Bluetooth disconnected: " + deviceName);
        }
    }
}
