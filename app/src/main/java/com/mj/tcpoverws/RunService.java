package com.mj.tcpoverws;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;

import androidx.core.app.NotificationCompat;

import tcp_over_ws_lib.LogInterface;
import tcp_over_ws_lib.Tcp_over_ws_lib;

public class RunService extends Service implements LogInterface {

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, getNotification());
    }

    private Notification getNotification() {
        String channelId = "my_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "TcpOverWs Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("TcpOverWs Service is Running")
                .setContentText("TcpOverWs Service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Tcp_over_ws_lib.setLogger(this);
        // 在 Service 中启动 Go 代码
        new Thread(() -> {
            try {
                // 调用 Tcp_over_ws_lib.run() 方法，假设这是 Go 代码的入口
                System.out.println("START GO!!!");
                Tcp_over_ws_lib.run(intent.getStringExtra("config"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("On DESTROY!!!");
        Process.killProcess(Process.myPid());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void logCallback(String s) {
        System.out.println(s);
        Intent broadcastIntent = new Intent("com.mj.tcpoverws.UPDATE_LOG");
        broadcastIntent.putExtra("message", s);
        sendBroadcast(broadcastIntent);
    }
}
