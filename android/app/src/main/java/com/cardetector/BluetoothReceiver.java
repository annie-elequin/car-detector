package com.cardetector;

import android.bluetooth.BluetoothDevice;
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
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device == null || device.getName() == null) {
            return;
        }

        WritableMap params = Arguments.createMap();
        params.putString("deviceName", device.getName());
        params.putString("deviceAddress", device.getAddress());

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            sendEvent("onBluetoothConnected", params);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            sendEvent("onBluetoothDisconnected", params);
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }
}
