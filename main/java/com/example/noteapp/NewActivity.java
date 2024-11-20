package com.example.noteapp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewActivity extends AppCompatActivity {
    private static final int max_num_notes = 500; // 最多的便签数量
    private static final int max_num_types = 100; // 最多的类别数量
    private static final String TAG = "NewActivity";
    private EditText editTextNote, editTitle;
    private Button button_type;
    private Button button_had_remind;
    private PopupWindow popupWindow;
//    private TextView textViewStatus;
    public String[] titles = new String[max_num_notes+5];
    public int[] vis_type = new int[max_num_types+5]; // 标记类别是否使用
    public String[] type_name = new String[max_num_types+5]; // 记录类别名称
    public int[] note_type_id = new int[max_num_notes+5]; // 记录便签类别
    public int[] vis_remind = new int[max_num_notes+5]; // 标记是否设置了提醒
    public long[] remind_time = new long[max_num_notes+5]; // 记录定时提醒的时间
    private int vis_remind_old = 0;
    private long remind_time_old = 0;
    private int note_id = 1;
    private AlarmManager alarmManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);

        editTextNote = findViewById(R.id.editTextNote2);
        editTitle = findViewById(R.id.editTitle);
        ImageButton button_back = findViewById(R.id.button_back);

        button_type = findViewById(R.id.button_type);
        // 包含pop_button_delete 和 button_remind
        ImageButton button_option = findViewById(R.id.button_option);
        button_had_remind = findViewById(R.id.button_had_remind);
//        textViewStatus = findViewById(R.id.textViewStatus2);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);//定时任务

        Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
        editTitle.setTypeface(boldTypeface);

        // 尝试读取文件内容并设置到EditText中
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String value = sharedPreferences.getString("globalVariable", "");
        note_id=Integer.parseInt(value);


        type_name = readStrArray("type_name.txt");
        note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
        String ans="";
        int type_this = note_type_id[note_id];
        if(type_this==0){
            ans = "未分类笔记";
        }else if(note_id>=1){
            ans = type_name[type_this];
        }
        ans = ans+" ˅";
        button_type.setText(ans);


        vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
        if(vis_remind[note_id]==1){
            button_had_remind.setVisibility(View.VISIBLE);
        }else{ // vis_remind[note_id]==0
            button_had_remind.setVisibility(View.INVISIBLE);
        }
        vis_remind_old = vis_remind[note_id]; // 更新vis_remind_old

//        remind_time = readLongArray("remind_time.txt");
        remind_time = FileUtils.readLongArray(getApplicationContext(), "remind_time.txt");

        remind_time_old = remind_time[note_id]; // 更新remind_time_old


        titles = new String[max_num_notes+5];
        titles = readStrArray("title.txt");

//        if(titles[note_id]!=""){
//        if(titles[note_id].equals("")){
        if(!titles[note_id].equals("")){ // 目前标题不为空
            editTitle.setText(titles[note_id]);
        }else{ // titles[note_id].equals(""), 目前标题为空
            editTitle.setHint("标题");
        }

        String filename = "note"+value+".txt"; //
        String fileContents = readNoteFromFile(filename);
        if (fileContents != null) {
            editTextNote.setText(fileContents);
        }

        button_back.setOnClickListener(v -> { // 自动保存并返回
            auto_save_and_ck();
            finish(); // 退出当前Activity
        });

        Button buttonSave = findViewById(R.id.button_save);

        buttonSave.setOnClickListener(v -> { // 手动保存
            String noteContent = editTextNote.getText().toString();

            SharedPreferences sharedPreferences1 = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String value1 = sharedPreferences1.getString("globalVariable", "");
            note_id = Integer.parseInt(value1);

            saveNoteToFile(noteContent, note_id);

            titles[note_id] = editTitle.getText().toString();
            writeStrArray("title.txt", titles);

            long currentTimeMillis = System.currentTimeMillis();
//                Log.d(TAG, "onClick, currentTimeMillis = "+currentTimeMillis);

            long[] timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");

            timeMillis[note_id] = currentTimeMillis; // 更新时间戳
//            writeLongArray("timeMillis.txt", timeMillis);
            FileUtils.writeLongArray(getApplicationContext(), "timeMillis.txt", timeMillis);


            editTitle.clearFocus(); // 标题输入框失去焦点
            editTextNote.clearFocus(); //
            View rootView = findViewById(android.R.id.content);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0); // 使用根视图的窗口令牌来隐藏输入法

//                textViewStatus.setText("便签已保存！");
        });

        //
        button_type.setOnClickListener(v -> { // 设置便签类别
            showCustomDialog();
        });

        Button button_fill = findViewById(R.id.button_fill);
        //
        button_fill.setOnClickListener(v -> {
            // 获取焦点到文本框末尾
            // 获取EditText中的文本长度
            int textLength = editTextNote.getText().length();
            // 将光标移动到文本末尾
            editTextNote.setSelection(textLength);
            // 请求焦点以确保EditText是活动的
            editTextNote.requestFocus();

            // 获取InputMethodManager实例
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

            // 尝试显示输入法
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(editTextNote, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        button_option.setOnClickListener(this::showPopupWindow);

        button_had_remind.setOnClickListener(this::showRemindDialog);
    }

    private void showRemindDialog(View v){
        Log.d(TAG,"showRemindDialog");

        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_modify_remind, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true); // 是否可以通过点击对话框外部取消

        // 创建对话框
        Dialog dialog = builder.create();

        // 调整位置
        Window mWindow = dialog.getWindow();
        assert mWindow != null;
        WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.dimAmount =0.0f;
        lp.x = 0; // 新位置X坐标
        lp.y = 750; // 新位置Y坐标
        dialog.onWindowAttributesChanged(lp);

        dialog.show(); // 显示对话框

        TextView textViewTime = dialogView.findViewById(R.id.textViewTime);

        long time1 = remind_time[note_id];
        Date date = new Date(time1); // 创建Date对象
        // 定义日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(date); // 将Date对象格式化为字符串

        long time_now = System.currentTimeMillis();
        Date date2 = new Date(time_now);
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate2 = sdf2.format(date2);

        int F_same=1;
        for(int i=0;i<=9;i++){
            if(formattedDate.charAt(i) != formattedDate2.charAt(i)){
                F_same=0;
                break;
            }
        }

        StringBuilder ans= new StringBuilder();
        if(F_same==1){ // 日期相同，就是今天
            ans = new StringBuilder("今天");
            for(int i=10;i<=15;i++){
                ans.append(formattedDate.charAt(i));
            }
        }else{
            for(int i=0;i<=15;i++){
                ans.append(formattedDate.charAt(i));
            }
        }
        if(time_now >= time1){ // 当前时间已经过了设定时间
            ans.append(" 已过期");
        }
        textViewTime.setText(ans.toString());
        Button button_cancel = dialogView.findViewById(R.id.button_cancel);
        button_cancel.setOnClickListener(v1 -> {
//                Log.d(TAG,"关闭对话框");
            dialog.dismiss();
        });

        Button button_remove_remind = dialogView.findViewById(R.id.button_remove_remind);
        button_remove_remind.setOnClickListener(v12 -> {
            Log.d(TAG,"移除提醒"); //
            cancelReminder();

            // 更新vis_remind
            vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
            if(vis_remind[note_id]==1){
                vis_remind[note_id]=0;
                // writeIntArrayToFile("vis_remind.txt", vis_remind);
                FileUtils.writeIntArray(getApplicationContext(), "vis_remind.txt", vis_remind);
                vis_remind_old = 0;
                button_had_remind.setVisibility(View.INVISIBLE); // 设置按钮不可见
            }

            dialog.dismiss();
        });

        Button button_reset_remind = dialogView.findViewById(R.id.button_reset_remind);
        button_reset_remind.setOnClickListener(v13 -> {
            Log.d(TAG,"重新设置提醒"); //

            // 跳转设置提醒的界面
            Intent intent = new Intent(NewActivity.this, RemindActivity.class);
            startActivity(intent);

            dialog.dismiss();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
//        remind_time = readLongArray("remind_time.txt");
        remind_time = FileUtils.readLongArray(getApplicationContext(), "remind_time.txt");

        //
        Log.d(TAG,"note_id = "+note_id);
        Log.d(TAG,"vis_remind[note_id] = "+vis_remind[note_id]);

        button_had_remind = findViewById(R.id.button_had_remind);

        Log.d(TAG,"button_had_remind.getVisibility() = "+ button_had_remind.getVisibility());
        if(vis_remind[note_id]==1){
            button_had_remind.setVisibility(View.VISIBLE);
            //
            Log.d(TAG,"已变成可见的");
        }else{ // vis_remind[note_id]==0
            button_had_remind.setVisibility(View.INVISIBLE);
            //
            Log.d(TAG,"已变成不可见的");
        }

        Log.d(TAG,"button_had_remind.getVisibility() = "+ button_had_remind.getVisibility());


        if(vis_remind[note_id]!=vis_remind_old){ // 设置提醒的状态发生了变化
            if(vis_remind[note_id]==1){ // 0->1
//                button_had_remind.setVisibility(View.VISIBLE);

                // 新建定时提醒
                Log.d(TAG, "设置定时提醒");
                setReminder(getApplicationContext(),remind_time[note_id]); //

            }else{ // vis_remind[note_id]==0, 1->0
                // 取消便签提醒
                cancelReminder(); //

//                button_had_remind.setVisibility(View.INVISIBLE);
            }
        }else if(vis_remind[note_id]==1){ // 1->1
            // 判断提醒时间戳是否发生变化
            if(remind_time[note_id] != remind_time_old){ // 设置提醒的时间戳发生变化，修改提醒
                Log.d(TAG, "设置提醒的时间戳发生变化，修改提醒");

                // 取消原来的定时提醒
                cancelReminder();

                // 重新设置定时提醒
                setReminder(getApplicationContext(),remind_time[note_id]); //
            }
        }

        vis_remind_old = vis_remind[note_id]; // 更新vis_remind_old
        remind_time_old = remind_time[note_id]; // 更新remind_time_old
    }

    public void cancelReminder(){
        try{
            Context context1 = getApplicationContext();
            Intent i0 = new Intent(context1, ReminderReceiver.class);
//                    PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), note_id, i0, PendingIntent.FLAG_MUTABLE);
            PendingIntent pending = PendingIntent.getBroadcast(context1, note_id, i0, PendingIntent.FLAG_MUTABLE);
            alarmManager.cancel(pending); // 移除提醒

            Log.d(TAG,"移除提醒成功");
        }catch (Exception e){
            Log.d(TAG,"移除提醒时出错");
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    public void setReminder(Context context, long reminderTime) { // 设置提醒
        Log.d(TAG, "setReminder, reminderTime = "+reminderTime);

        Intent intent = new Intent(context, ReminderReceiver.class);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String value = sharedPreferences.getString("globalVariable", "");
        note_id = Integer.parseInt(value); // 获得当前便签的id

        String[] titles=readStrArray("title.txt");

        String myString = titles[note_id];
        //
//        Log.d(TAG,"myString = "+myString);
        intent.putExtra("title_key", myString); // 将字符串信息附加到 Intent 中

        PendingIntent pendingIntent;
//            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT); // 会报错

        pendingIntent = PendingIntent.getBroadcast(context, note_id, intent, PendingIntent.FLAG_MUTABLE); // base Intent可以改写
//        pendingIntent = PendingIntent.getBroadcast(context, note_id, intent, PendingIntent.FLAG_IMMUTABLE); // base Intent不能改写

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        boolean hasPermission = false; // true:有权限,false:没有权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            hasPermission = alarmManager.canScheduleExactAlarms();
        }

        if(hasPermission){
            // Log.d(TAG, "alarmManager.setExact start");
            try{
                // 设定精确的提醒时间
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent); // 缺少相关权限时会报错
//                Log.d(TAG, "alarmManager.setExact successfully, reminderTime = "+reminderTime);

            }catch (Exception e){
                Log.d(TAG, "Exception e = "+e);
                Log.d(TAG, "ERROR on alarmManager.setExact");
                Log.d(TAG, "AlarmManager.RTC_WAKEUP = "+AlarmManager.RTC_WAKEUP);
                Log.d(TAG, "reminderTime = "+reminderTime);
                Log.d(TAG, "pendingIntent = "+pendingIntent);
            }
        }else{
            Log.d(TAG, "没有闹钟权限");
        }
    }

    private void showPopupWindow(View anchorView) {
        // 加载popup_layout.xml布局文件
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_layout, null);

        // 创建PopupWindow对象
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // 设置动画
//        popupWindow.setAnimationStyle(R.style.PopupEnrollAnimation);

        // 背景阴影
//        ColorDrawable dw = new ColorDrawable(0x80000000);
//        popupWindow.setBackgroundDrawable(dw);

        // 设置PopupWindow的背景
//        popupWindow.setBackgroundDrawable(new ColorDrawable(0xFFFCFCFC));

        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.popup_window_border));

        // 设置PopupWindow的焦点
        popupWindow.setFocusable(true);

        // 设置PopupWindow外部点击是否消失
        popupWindow.setOutsideTouchable(true);

        // 设置PopupWindow显示的位置和偏移量
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], location[1] + anchorView.getHeight());

        Drawable drawable1 = ResourcesCompat.getDrawable(getResources(), R.drawable.button_remind, null);
        assert drawable1 != null;
        drawable1.setBounds(20, 0, 80, 60);
        Button button_remind = popupView.findViewById(R.id.button_remind);
        button_remind.setCompoundDrawables(drawable1, null, null, null);// 只放左边

        Drawable drawable2 = ResourcesCompat.getDrawable(getResources(), R.drawable.button_delete, null);
        assert drawable2 != null;
        drawable2.setBounds(20, 0, 80, 60);
        Button pop_button_delete = popupView.findViewById(R.id.button_delete);
        pop_button_delete.setCompoundDrawables(drawable2, null, null, null);// 只放左边

        // 为PopupWindow中的按钮设置点击事件
        pop_button_delete.setOnClickListener(v -> {
            // 处理按钮点击事件
            popupWindow.dismiss(); // 隐藏popupWindow

            // 删除便签
            // 创建并显示一个 AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(NewActivity.this);
            builder.setTitle("确定删除此笔记吗?")
                    .setMessage("")
                    .setCancelable(false)
                    .setPositiveButton("删除", (dialog, id2) -> {
                        // 用户点击了 Yes 按钮，执行删除操作
                        try{
                            editTextNote.setText("");
                            editTitle.setText("");

                            int[] vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");
                            vis[note_id]=0; // 标记为删除
                            FileUtils.writeIntArray(getApplicationContext(), "vis.txt", vis);

                            String[] titles=readStrArray("title.txt");
                            titles[note_id]=""; // 清空标题
                            writeStrArray("title.txt", titles);

                            long[] timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");
                            timeMillis[note_id]=0; // 清空时间戳
                            FileUtils.writeLongArray(getApplicationContext(), "timeMillis.txt", timeMillis);

                            saveNoteToFile("", note_id); // 清空内容

                            note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
                            note_type_id[note_id]=0; // 删除对应的类别id
                            FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);

                            // 更新提醒相关的全局变量，并取消对应的提醒
                            remind_time = FileUtils.readLongArray(getApplicationContext(), "remind_time.txt");
                            remind_time[note_id] = 0;
                            FileUtils.writeLongArray(getApplicationContext(), "remind_time.txt", remind_time);

                            vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
                            vis_remind[note_id] = 0;
                            FileUtils.writeIntArray(getApplicationContext(), "vis_remind.txt", vis_remind);

                            cancelReminder(); // 取消对应的提醒

                            finish(); // 退出当前Activity

                        }catch(Exception e){
                            Log.d(TAG, "ERROR on Delete");
                        }
                    })
                    .setNegativeButton("取消", (dialog, id2) -> {
                        // 用户点击了 No 按钮，取消操作
                        dialog.cancel();
                    });
            // 显示对话框
            AlertDialog alert = builder.create();
            alert.show();
        });


        button_remind.setOnClickListener(v -> {
            // 处理按钮点击事件
            popupWindow.dismiss(); // 隐藏popupWindow
            //
//                Log.d(TAG,"onClick, 设置提醒");

            // 跳转设置提醒的界面
            Intent intent = new Intent(NewActivity.this, RemindActivity.class);
            startActivity(intent);
        });
    }

    public void auto_save_and_ck(){
        // 自动保存
        String noteContent = editTextNote.getText().toString();
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String value = sharedPreferences.getString("globalVariable", "");
        note_id = Integer.parseInt(value);
//                Log.d(TAG, "onClick, id = "+id);

        saveNoteToFile(noteContent, note_id);
        titles[note_id] = editTitle.getText().toString();
        writeStrArray("title.txt", titles);

        // 检测并删除没有设置提醒的空标签
        if(titles[note_id].length()==0 && noteContent.length()==0){ // 完全是空的
            if(vis_remind[note_id]==0){ // 没有设置提醒
                // 删除这个便签
                try{
                    int[] vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");
                    vis[note_id]=0; // 标记为删除
                    FileUtils.writeIntArray(getApplicationContext(), "vis.txt", vis);

                    String[] titles=readStrArray("title.txt");
                    titles[note_id]=""; // 清空标题
                    writeStrArray("title.txt", titles);

                    long[] timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");
                    timeMillis[note_id]=0; // 清空时间戳
                    FileUtils.writeLongArray(getApplicationContext(), "timeMillis.txt", timeMillis);

                    saveNoteToFile("", note_id); // 清空内容

                    note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
                    note_type_id[note_id]=0; // 删除对应的类别id
                    FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);

                }catch(Exception e){
                    Log.d(TAG, "ERROR on Delete");
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        auto_save_and_ck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy() 被调用"); //
        auto_save_and_ck();
    }

    private void showCustomDialog() { // 设置便签类别
//        Log.d(TAG,"showCustomDialog()"); //

        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true); // 是否可以通过点击对话框外部取消


        // 创建对话框
        Dialog dialog = builder.create();

        // 调整位置
        Window mWindow = dialog.getWindow();
        assert mWindow != null;
        WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.dimAmount =0.0f;
        lp.x = 0; // 新位置X坐标
        lp.y = 750; // 新位置Y坐标
        dialog.onWindowAttributesChanged(lp);

        dialog.show(); // 显示对话框

        vis_type = FileUtils.readIntArray(getApplicationContext(), "vis_type.txt");
        if(vis_type==null){
            Log.d(TAG,"vis_type==null"); //
        }else{
            addType(dialogView, 0); // 添加未分类笔记的按钮
            for(int i=1;i<=max_num_types;i++){
                if(vis_type[i]==1){
                    addType(dialogView, i); // 添加类别按钮
                }
            }
        }

        Button button_new_type  = dialogView.findViewById(R.id.button_new_type);
        button_new_type.setOnClickListener(v -> {
            showNewTypeDialog(dialogView); // 新建类别
        });
    }

    private void addType(View dialogView, int id) { // 添加序号为id的类别按钮
        if(id<0)return;

        Button noteButton = new Button(this); // 创建新按钮
        noteButton.setTransformationMethod(null); // 禁用文本大写转换

        if(id==0){
            noteButton.setText("未分类笔记");
        }else{ // id>=1
            type_name = readStrArray("type_name.txt");
            noteButton.setText(type_name[id]);
        }

        noteButton.setTypeface(null, Typeface.BOLD);
        noteButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); // 靠左对齐（同时垂直居中）

        noteButton.setLayoutParams(new LinearLayout.LayoutParams( // 设置按钮的布局参数
                LinearLayout.LayoutParams.MATCH_PARENT, // 宽度
                // 500, // 宽度

                200 // 高度
                // LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Set an OnClickListener for the new button
        noteButton.setOnClickListener(v -> { // 选择类别
//                // 按钮的点击事件处理
            type_name = readStrArray("type_name.txt");

            String s1;
            if(id==0){
                s1 = "未分类笔记";
            }else{ // id>=1
                s1 = type_name[id];
            }
            Toast.makeText(NewActivity.this, "已设置为类别\""+s1+"\"", Toast.LENGTH_SHORT).show();

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String value = sharedPreferences.getString("globalVariable", "");
            note_id = Integer.parseInt(value);
//
            note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
            note_type_id[note_id] = id; // 设置便签类别
//            writeIntArrayToFile("note_type_id.txt", note_type_id);
            FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);

            // 更新界面
            type_name = readStrArray("type_name.txt");
            String ans="";
            int type_this = note_type_id[note_id];
            if(type_this==0){
                ans = "未分类笔记";
            }else if(note_id>=1){
                ans = type_name[type_this];
            }
            ans = ans+" ˅";
            button_type.setText(ans);

//            dialogView.finish();
        });

        LinearLayout noteButtonsContainer = dialogView.findViewById(R.id.note_buttons_container);
        // Add the new button to the container
        noteButtonsContainer.addView(noteButton);

    }
    private void showNewTypeDialog(View dialogView_father) {
        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_type, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true) // 是否可以通过点击对话框外部取消
                .setPositiveButton("确定", (dialog, id) -> {
                    // 用户点击 OK 按钮后的操作
                    EditText editText = dialogView.findViewById(R.id.editTextText);
                    String typeNameThis = editText.getText().toString();

                    if(typeNameThis.length()>0){
                        // 判断是否没有相同名称的类别
                        //
                        int has_same = 0;
                        for(int i=1;i<=max_num_types;i++){
                            if(vis_type[i]==1){
                                if(type_name[i].equals(typeNameThis)){ // 名称相同
                                    has_same = 1;
                                    break;
                                }
                            }
                        }

                        if(has_same == 0){ // 没有相同名称的类别
                            // 新建类别
                            Toast.makeText(NewActivity.this, "新建类别 "+typeNameThis, Toast.LENGTH_SHORT).show();

                            type_name = readStrArray("type_name.txt");
                            vis_type = FileUtils.readIntArray(getApplicationContext(), "vis_type.txt");
                            int num1=1;
                            for(int i=1;i<=max_num_types;i++){
                                if(vis_type[i]==0){
                                    type_name[i] = typeNameThis; // 设置类别名称
                                    vis_type[i]=1; // 标记为已使用
                                    num1 = i;
                                    break;
                                }
                            }
                            writeStrArray("type_name.txt",type_name);
//                        writeIntArrayToFile("vis_type.txt", vis_type);
                            FileUtils.writeIntArray(getApplicationContext(),"vis_type.txt", vis_type);

                            addType(dialogView_father, num1); // 更新界面

                            Log.d(TAG, "typeNameThis = "+typeNameThis);
                            Log.d(TAG, "typeNameThis.length() = "+typeNameThis.length());
                        }else{ // 类别名称已存在
                            Toast.makeText(NewActivity.this, "类别名称已存在", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        Toast.makeText(NewActivity.this, "类别名称不能为空", Toast.LENGTH_SHORT).show();
                    }

                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, id) -> {
                    // 用户点击取消按钮后的操作
                    dialog.dismiss();
                });

        // 创建并显示对话框
        Dialog dialog = builder.create();
        dialog.show();

        EditText editText = dialogView.findViewById(R.id.editTextText);
        editText.setHint("输入类别名称");

    }

    private String readNoteFromFile(String filename) {
        //
//        Log.d(TAG, "readNoteFromFile, filename = "+filename);

        FileInputStream fis = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            fis = openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            // 处理读取失败的情况，例如显示错误消息或记录日志
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void saveNoteToFile(String content, int noteId) {
        String filename = "note"+noteId+".txt";
        //
//        Log.d(TAG, "saveNoteToFile, filename = "+filename);

        FileOutputStream fos = null;
        try {
            fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(content.getBytes());
            // 可选：提示用户保存成功
        } catch (IOException e) {
            e.printStackTrace();
            // 处理保存失败的情况，例如显示错误消息
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeStrArray(String fileName, String[] s) {
        try (FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String[] readStrArray(String fileName) {
        String[] s = null;
        try (FileInputStream fis = openFileInput(fileName);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            s = (String[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return s;
    }

}
