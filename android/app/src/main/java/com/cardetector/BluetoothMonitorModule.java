package com.cardetector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
            // Prefer the live audio route — this is what "connected to my car" usually means.
            AudioManager audioManager =
                (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                AudioDeviceInfo[] outputs =
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo info : outputs) {
                    int type = info.getType();
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                        || type == AudioDeviceInfo.TYPE_BLE_SPEAKER) {
                        CharSequence product = info.getProductName();
                        String name = product != null ? product.toString() : "Bluetooth audio";
                        if (name.trim().isEmpty()) {
                            name = "Bluetooth audio";
                        }
                        WritableMap deviceInfo = Arguments.createMap();
                        deviceInfo.putString("name", name);
                        deviceInfo.putString("address", "");
                        deviceInfo.putString("source", "audio_route");
                        promise.resolve(deviceInfo);
                        return;
                    }
                }
            }

            // Fallback: devices actively connected on A2DP or headset profiles.
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                promise.resolve(null);
                return;
            }

            BluetoothDevice connected = firstConnectedProfileDevice(adapter);
            if (connected != null) {
                String name = null;
                try {
                    name = connected.getName();
                } catch (SecurityException e) {
                    promise.reject(
                        "PERMISSION",
                        "Bluetooth permission not granted. Allow Nearby devices for this app.");
                    return;
                }
                if (name == null || name.trim().isEmpty()) {
                    name = connected.getAddress();
                }
                WritableMap deviceInfo = Arguments.createMap();
                deviceInfo.putString("name", name);
                deviceInfo.putString("address", connected.getAddress());
                deviceInfo.putString("source", "bluetooth_profile");
                promise.resolve(deviceInfo);
                return;
            }

            promise.resolve(null);
        } catch (SecurityException e) {
            promise.reject(
                "PERMISSION",
                "Bluetooth permission not granted. Allow Nearby devices for this app.");
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    private BluetoothDevice firstConnectedProfileDevice(BluetoothAdapter adapter) {
        AtomicReference<BluetoothDevice> found = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(2);

        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                try {
                    List<BluetoothDevice> devices = proxy.getConnectedDevices();
                    if (found.get() == null && devices != null && !devices.isEmpty()) {
                        found.set(devices.get(0));
                    }
                } catch (SecurityException ignored) {
                } finally {
                    try {
                        adapter.closeProfileProxy(profile, proxy);
                    } catch (Exception ignored) {
                    }
                    latch.countDown();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                latch.countDown();
            }
        };

        try {
            adapter.getProfileProxy(reactContext, listener, BluetoothProfile.A2DP);
            adapter.getProfileProxy(reactContext, listener, BluetoothProfile.HEADSET);
            latch.await(1500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
        return found.get();
    }

    @ReactMethod
    public void listBondedDevices(Promise promise) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                promise.resolve(Arguments.createArray());
                return;
            }
            WritableArray arr = Arguments.createArray();
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                WritableMap map = Arguments.createMap();
                String name;
                try {
                    name = device.getName();
                } catch (SecurityException e) {
                    promise.reject(
                        "PERMISSION",
                        "Bluetooth permission not granted. Allow Nearby devices for this app.");
                    return;
                }
                map.putString("name", name != null ? name : device.getAddress());
                map.putString("address", device.getAddress());
                arr.pushMap(map);
            }
            promise.resolve(arr);
        } catch (SecurityException e) {
            promise.reject(
                "PERMISSION",
                "Bluetooth permission not granted. Allow Nearby devices for this app.");
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void startMonitoring(String deviceName) {
        monitoredDeviceName = deviceName;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

        try {
            reactContext.unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered yet
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reactContext.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            reactContext.registerReceiver(bluetoothReceiver, filter);
        }

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
