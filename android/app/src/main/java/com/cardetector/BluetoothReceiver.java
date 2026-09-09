package com.cardetector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class BluetoothReceiver extends BroadcastReceiver {
    private final ReactApplicationContext reactContext;

    public BluetoothReceiver(ReactApplicationContext context) {
        this.reactContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String name = null;
        String address = null;
        if (device != null) {
            try {
                name = device.getName();
            } catch (SecurityException e) {
                name = null;
            }
            try {
                address = device.getAddress();
            } catch (SecurityException e) {
                address = null;
            }
            if (name == null || name.trim().isEmpty()) {
                name = address != null ? address : "Bluetooth device";
            }
        }

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            emit("onBluetoothConnected", name, address);
            return;
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            emit("onBluetoothDisconnected", name, address);
            return;
        }

        if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)
            || BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                emit("onBluetoothConnected", name, address);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                emit("onBluetoothDisconnected", name, address);
            }
        }
    }

    private void emit(String eventName, String name, String address) {
        if (name == null) {
            return;
        }
        WritableMap params = Arguments.createMap();
        params.putString("deviceName", name);
        params.putString("deviceAddress", address != null ? address : "");
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }
}
