package com.example.noteapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RemindActivity extends AppCompatActivity {
    private static final String TAG = "RemindActivity";
    private static final String CHANNEL_ID = "NoteRemind";
    private static final int max_num_notes = 500; // 最多的便签数量

    private EditText editTextTime;

    public int[] vis_remind = new int[max_num_notes+5]; // 标记是否设置了提醒
    public long[] remind_time = new long[max_num_notes+5]; // 记录定时提醒的时间
    private int note_id = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remind);

//        Log.d(TAG,"RemindActivity, onCreate");

        Button button_cancel = findViewById(R.id.button_cancel);
        Button button_finish = findViewById(R.id.button_finish);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String value = sharedPreferences.getString("globalVariable", "");
        note_id=Integer.parseInt(value);


//        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // 创建通知通道
        createNotificationChannel();

//        Log.d(TAG, "onCreate, note_id = "+note_id);

        button_cancel.setOnClickListener(v -> { // 返回
            finish(); // 退出当前Activity
        });

        Context context1 = this;

        button_finish.setOnClickListener(v -> { // 设置提醒

            editTextTime = findViewById(R.id.editTextTime);
//                Log.d(TAG,"editTextTime = "+editTextTime);

            String time2_str = editTextTime.getText().toString();
            Log.d(TAG,"time2_str = "+time2_str); // 类似于2024:11:18:13:04

            long time2 = convertTime(time2_str);
            //
            Log.d(TAG,"time2 = "+time2); // 时间戳, 类似于1731935040000
            if(time2 != -1){

//                vis_remind = readIntArrayFromFile("vis_remind.txt");
                vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
                vis_remind[note_id]=1; // 标记设置了提醒
                // writeIntArrayToFile("vis_remind.txt", vis_remind);
                FileUtils.writeIntArray(getApplicationContext(), "vis_remind.txt", vis_remind);

                //
                Log.d(TAG,"note_id ="+note_id);
                Log.d(TAG,"vis_remind[note_id] ="+vis_remind[note_id]);

//                remind_time = readLongArray("remind_time.txt");
                remind_time = FileUtils.readLongArray(getApplicationContext(), "remind_time.txt");
                remind_time[note_id]=time2; // 更新提醒时间
//                writeLongArray("remind_time.txt", remind_time);
                FileUtils.writeLongArray(getApplicationContext(), "remind_time.txt", remind_time);

                Toast.makeText(context1, "已设置提醒", Toast.LENGTH_SHORT).show(); //

            }else{ // time2 == -1
                Log.d(TAG,"转换时间戳时出错，时间解析失败");
                Toast.makeText(context1, "时间解析失败", Toast.LENGTH_SHORT).show();
            }

            finish(); // 退出当前Activity
        });

    }

    public static long convertTime(String timeString) {
        try{
            // 分割字符串
            String[] parts = timeString.split(":");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            int hour = Integer.parseInt(parts[3]);
            int minute = Integer.parseInt(parts[4]);
            int second = 0;

            // 创建 LocalDateTime 对象
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
                // 转换为 ZonedDateTime 对象（假设使用系统默认时区）
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());

                // 转换为 Instant 对象
                Instant instant = zonedDateTime.toInstant();

                // 转换为时间戳（毫秒）
                return instant.toEpochMilli();
            }else{
                Log.d(TAG,"ERROR on 创建LocalDateTime对象, android版本过低");
                return -1;
            }
        }catch (Exception e){
            Log.d(TAG,"转换时间戳时出错, timeString = "+timeString);
            return -1;
        }
    }

    // 创建通知通道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "便签提醒";
            String description = "便签提醒的通知通道";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
