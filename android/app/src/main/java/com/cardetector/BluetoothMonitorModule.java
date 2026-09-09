package com.cardetector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Set;

public class BluetoothMonitorModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private BluetoothReceiver bluetoothReceiver;
    private String monitoredDeviceName = null;

    public BluetoothMonitorModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
        this.bluetoothReceiver = new BluetoothReceiver(context);
    }

    @Override
    public String getName() {
        return "BluetoothMonitor";
    }

    @ReactMethod
    public void getCurrentConnection(Promise promise) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                promise.resolve(null);
                return;
            }

            // Get connected Bluetooth devices
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            // Check audio profile connections
            for (BluetoothDevice device : pairedDevices) {
                // In a production app, you'd check A2DP profile connection state
                // For this spike, we'll return the first paired device
                if (device.getName() != null) {
                    WritableMap deviceInfo = Arguments.createMap();
                    deviceInfo.putString("name", device.getName());
                    deviceInfo.putString("address", device.getAddress());
                    promise.resolve(deviceInfo);
                    return;
                }
            }
            
            promise.resolve(null);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void startMonitoring(String deviceName) {
        monitoredDeviceName = deviceName;
        
        // Register broadcast receiver for Bluetooth events
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        
        try {
            reactContext.unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered yet
        }
        
        reactContext.registerReceiver(bluetoothReceiver, filter);
        
        // Start foreground service for background monitoring
        Intent serviceIntent = new Intent(reactContext, BluetoothMonitorService.class);
        serviceIntent.putExtra("deviceName", deviceName);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            reactContext.startForegroundService(serviceIntent);
        } else {
            reactContext.startService(serviceIntent);
        }
    }

    @ReactMethod
    public void stopMonitoring() {
        monitoredDeviceName = null;
        
        try {
            reactContext.unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Already unregistered
        }
        
        Intent serviceIntent = new Intent(reactContext, BluetoothMonitorService.class);
        reactContext.stopService(serviceIntent);
    }

    @ReactMethod
    public void isMonitoringEnabled(Promise promise) {
        promise.resolve(monitoredDeviceName != null);
    }

    public void sendEvent(String eventName, WritableMap params) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }
}
