package com.example.noteapp;

import static android.content.Context.NOTIFICATION_SERVICE;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "NoteRemind";

    private NotificationManager manager;

    @Override
    public void onReceive(Context context, Intent intent) {
        //
//        Log.d(TAG, "onReceive, 触发了定时提醒");

        String title="通知标题";
        if (intent != null && intent.hasExtra("title_key")) {
            // 使用 receivedString 做一些事情
            title = intent.getStringExtra("title_key");
        }else{
            Log.d(TAG, "ERROR on getting title, intent = "+intent);
            assert intent != null;
            Log.d(TAG, "intent.hasExtra(\"title_key\") = "+intent.hasExtra("title_key"));
        }

        sendNotification(context, title);
    }

    public void sendNotification(Context context, String title) {
//        Log.d(TAG, "sendNotification(), 发送通知"); //

        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                NotificationChannel channel=new NotificationChannel("id1",CHANNEL_ID,
                        NotificationManager.IMPORTANCE_HIGH);

                try{
                    manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    manager.createNotificationChannel(channel); //
                }catch (Exception e){
                    Log.d(TAG, "ERROR on manager.createNotificationChannel(channel)");
                    Log.d(TAG, "manager = "+manager);
                    Log.d(TAG, "Exception e = "+e);
                }
            }

            Notification note = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
//                    .setContentTitle("通知标题")
                    .setContentText("便签提醒")
                    .setSmallIcon(R.drawable.ic_notification_v2)
                    .build();

            manager.notify(1, note);

        }catch (Exception e){
            Log.d(TAG, "ERROR on sendNotification()");
        }

    }
}