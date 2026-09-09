package com.cardetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class BluetoothMonitorService extends Service {
    private static final String CHANNEL_ID = "CarDetectorChannel";
    private static final int NOTIFICATION_ID = 1;
    private BluetoothServiceReceiver bluetoothReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        bluetoothReceiver = new BluetoothServiceReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceName = intent != null ? intent.getStringExtra("deviceName") : "your car";
        
        // Create notification for foreground service
        Notification notification = createNotification(deviceName);
        startForeground(NOTIFICATION_ID, notification);

        // Register receiver for Bluetooth events
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(bluetoothReceiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Car Connection Monitoring",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors your car's Bluetooth connection");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String deviceName) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Car Detector Active")
            .setContentText("Monitoring connection to " + deviceName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
}
