package com.example.noteapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.app.AlertDialog;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class Note implements Comparable<Note> {
    private final long time;
    private final int id;

    public Note(long time, int id) {
        this.time = time;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compareTo(Note other) {
        // 按时间戳降序排序
        return Long.compare(other.time, this.time);
    }
}

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int max_num_notes = 500; // 最多的便签数量
    private static final int max_num_types = 100; // 最多的类别数量
    private static final String CHANNEL_ID = "NoteRemind";
    private LinearLayout noteButtonsContainer;
    public int buttonCounter = 0;
    private TextView myTextView, textViewNoteType;
    public int[] vis = new int[max_num_notes+5]; // 标记便签是否使用
    public String[] titles = new String[max_num_notes+5];
    public long[] timeMillis = new long[max_num_notes+5]; // 记录创建时间 或 最近手动保存的时间
    public int[] vis_type = new int[max_num_types+5]; // 标记类别是否使用
    public String[] type_name = new String[max_num_types+5]; // 记录类别名称
//    public int type_num = 0; // 记录类别数量
    public int[] note_type_id = new int[max_num_notes+5]; // 记录便签类别

    public int[] vis_remind = new int[max_num_notes+5]; // 标记是否设置了提醒
    public long[] remind_time = new long[max_num_notes+5]; // 记录定时提醒的时间

    public int type_show = -1; // 展示的便签的类别, type_show==-1 : 展示全部笔记; type_show==0 : 展示未分类的笔记
    public void force_init(){ // 强制初始化
        type_show = -1;

        vis = new int[max_num_notes+5];
        FileUtils.writeIntArray(getApplicationContext(), "vis.txt", vis);
        buttonCounter=0;

        titles = new String[max_num_notes+5];
        writeStrArray("title.txt", titles);

        timeMillis = new long[max_num_notes+5];
//        writeLongArray("timeMillis.txt", timeMillis);
        FileUtils.writeLongArray(getApplicationContext(), "timeMillis.txt", timeMillis);

        type_name = new String[max_num_types+5];
        writeStrArray("type_name.txt", type_name);

        note_type_id = new int[max_num_notes+5];
        FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);

        vis_type = new int[max_num_types+5];
        FileUtils.writeIntArray(getApplicationContext(), "vis_type", vis_type);

        vis_remind = new int[max_num_notes+5];
        FileUtils.writeIntArray(getApplicationContext(), "vis_remind.txt", vis_remind);

        remind_time = new long[max_num_notes+5];
//        writeLongArray("remind_time.txt", remind_time);
        FileUtils.writeLongArray(getApplicationContext(), "remind_time.txt", remind_time);
    }

    public void init(){ // 初始化
        try{
                type_show = -1;
                vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");
                titles = readStrArray("title.txt");
//                timeMillis = readLongArray("timeMillis.txt");
                timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");

                type_name = readStrArray("type_name.txt");
                note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
                vis_type = FileUtils.readIntArray(getApplicationContext(), "vis_type.txt");
                vis_remind = FileUtils.readIntArray(getApplicationContext(), "vis_remind.txt");
//                remind_time = readLongArray("remind_time.txt");
                remind_time = FileUtils.readLongArray(getApplicationContext(), "remind_time.txt");

                if(remind_time == null){
                    remind_time = new long[max_num_notes+5];
//                    writeLongArray("remind_time.txt", remind_time);
                    FileUtils.writeLongArray(getApplicationContext(), "remind_time.txt", remind_time);
                }
                if(vis_remind == null){
                    vis_remind = new int[max_num_notes+5];
                    FileUtils.writeIntArray(getApplicationContext(), "vis_remind.txt", vis_remind);
                }
                if(type_name == null){
                    type_name = new String[max_num_types+5];
                    writeStrArray("type_name.txt", type_name);
                }
                if(vis_type == null){
                    vis_type = new int[max_num_types+5];
                    FileUtils.writeIntArray(getApplicationContext(), "vis_type.txt", vis_type);
                    type_name = new String[max_num_types+5]; //
                    writeStrArray("type_name.txt", type_name);
                }
                if(note_type_id == null){
                    note_type_id = new int[max_num_notes+5];
                    FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);
                }
                if(vis == null || titles==null || timeMillis==null){
                    force_init(); // 强制初始化
                }
        }catch(Exception e){ // 读取失败
                force_init(); // 强制初始化
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


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init(); // 初始化

        // 创建通知通道
        createNotificationChannel();

        boolean hasPermission = false; // true:有权限,false:没有权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            hasPermission = alarmManager.canScheduleExactAlarms();
        }

        if(!hasPermission){
            // 提示获取权限
            Uri uri = Uri.parse("package:"+this.getPackageName());
            Intent i0 = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,uri);
            Bundle bundle = new Bundle();
            bundle.putInt("key1", 200);
            // bundle.putInt("key2", 200);
            startActivity(i0, bundle);
        }

        vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");
        int c1=0;
        for(int i=1;i<=max_num_notes;i++){
            if(vis[i]==1){
                c1++;
            }
        }
        buttonCounter=c1;

        titles = readStrArray("title.txt");

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("globalVariable", "1");
        editor.apply();

        editor.putString("show_btn", "0");
        editor.apply();

        noteButtonsContainer = findViewById(R.id.note_buttons_container);

        myTextView = findViewById(R.id.textView);

        Button buttonAdd = findViewById(R.id.buttonAdd);
        //
        buttonAdd.setOnClickListener(v -> { // 添加便签
            int num1=1;
            for(int i=1;i<=max_num_notes;i++){
                if(vis[i]==0){
                    num1=i;
                    break;
                }
            }
            if(type_show>=1){ // 当前只显示某个类别
                note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");
                note_type_id[num1] = type_show;
                FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);
            }

            long currentTimeMillis = System.currentTimeMillis();
//            long[] timeMillis = readLongArray("timeMillis.txt");
            long[] timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");

            timeMillis[num1] = currentTimeMillis; // 设置时间戳
//            writeLongArray("timeMillis.txt", timeMillis);
            FileUtils.writeLongArray(getApplicationContext(), "timeMillis.txt", timeMillis);

            vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");
            titles = readStrArray("title.txt");

            vis[num1]=1; // 记录启用便签
            FileUtils.writeIntArray(getApplicationContext(), "vis.txt", vis); // 更新"vis.txt"文件

            buttonCounter++;
            myTextView = findViewById(R.id.textView);
            myTextView.setText(String.format(Locale.getDefault(),"%d篇笔记", buttonCounter));

            SharedPreferences sharedPreferences1 = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor1 = sharedPreferences1.edit();
            editor1.putString("globalVariable", String.valueOf(num1));
            editor1.apply();

            // 跳转新便签的界面
            Intent intent = new Intent(MainActivity.this, NewActivity.class);
            startActivity(intent);
            // 可以使用 intent.putExtra() 将一些数据传递给NoteActivity
        });

        textViewNoteType =  findViewById(R.id.textViewNoteType);

        FrameLayout clickable_area = findViewById(R.id.clickable_area);
//        textViewNoteType.setOnClickListener(v -> showTypeDialog());
        clickable_area.setOnClickListener(v -> showTypeDialog());

        if(type_show==-1){
            textViewNoteType.setText("全部笔记");
        }else if(type_show==0) {
            textViewNoteType.setText("未分类笔记");
        }else{
            textViewNoteType.setText(type_name[type_show]);
        }

        myTextView.setText(String.format(Locale.getDefault(), "%d篇笔记", buttonCounter));
    }

    private void showTypeDialog() {
//        Log.d(TAG,"showTypeDialog()"); //

        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.act_main_type_dialog, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true); // 是否可以通过点击对话框外部取消

        // 创建对话框
        Dialog dialog = builder.create();

        // 调整位置
        Window mWindow = dialog.getWindow();
        assert mWindow != null; // 断言mWindow非空
        WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.dimAmount =0.0f;
        lp.x = -450; // 新位置X坐标
        lp.y = 0; // 新位置Y坐标
        dialog.onWindowAttributesChanged(lp);

        dialog.show(); // // 显示对话框

        // 调整大小
        Window window = dialog.getWindow();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        window.setLayout(width*3/4,height);

        update_dialog(dialogView); //


        TextView textViewCreate = dialogView.findViewById(R.id.textViewCreate) ;
        textViewCreate.setOnClickListener(v -> {
            // 新建类别
            showNewTypeDialog(dialogView);
        });

        Button button_all_type = dialogView.findViewById(R.id.button_all_type);
        button_all_type.setOnClickListener(v -> {
            if(type_show != -1){
                type_show = -1; // // 展示全部笔记
                update_main();
            }
        });

        Button button_type_not_classified = dialogView.findViewById(R.id.button_type_not_classified);
        button_type_not_classified.setOnClickListener(v -> {
            if(type_show != 0){
                type_show = 0; // 展示未分类的笔记
                update_main();
            }
        });
    }

    // 新建类别
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
                            Toast.makeText(MainActivity.this, "新建类别 "+typeNameThis, Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(MainActivity.this, "类别名称已存在", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        Toast.makeText(MainActivity.this, "类别名称不能为空", Toast.LENGTH_SHORT).show();
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

    public int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void addType(View dialogView, int id) { // 添加序号为id的类别按钮
        if(id<=0)return;

        // Create a new button
        Button noteButton = new Button(this);
        noteButton.setTransformationMethod(null); // 禁用文本大写转换

        int color = ContextCompat.getColor(this, R.color.transparent);
        noteButton.setBackgroundColor(color);

        type_name = readStrArray("type_name.txt");
        noteButton.setText(type_name[id]);
        noteButton.setTypeface(null, Typeface.BOLD);
        noteButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); // 靠左对齐（同时垂直居中）

//        int widthInPx = dpToPx(250);
        int heightInPx = dpToPx(60);
        noteButton.setLayoutParams(new LinearLayout.LayoutParams( // 设置按钮的布局参数
                LinearLayout.LayoutParams.MATCH_PARENT, // 宽度
//                widthInPx,

                heightInPx // 长度
                // LinearLayout.LayoutParams.WRAP_CONTENT
        ));

//        // Set an OnClickListener for the new button
        noteButton.setOnClickListener(v -> { // 点击类别按钮，筛选出这个类别的标签
            Log.d(TAG,"筛选出类别id="+id+"的标签"); //
            if(type_show!=id){
                type_show = id;
                update_main(); // 更新主界面
            }
        });

        noteButton.setOnLongClickListener(v -> {
            Log.d("btn listener:","btn is longClicked!");
            showModifyDialog(id, dialogView);
            return true; // 按钮长按时过滤掉单击事件
        });


        LinearLayout layout2 = dialogView.findViewById(R.id.note_buttons_container);
        // Add the new button to the container
        layout2.addView(noteButton);
    }

    private void showModifyDialog(int type_id, View dialogView_fa) {
        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.act_main_type_modify, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true) // 是否可以通过点击对话框外部取消
                .setNegativeButton("返回", (dialog, id) -> {
                    // 用户点击 Cancel 按钮后的操作
                    dialog.dismiss();
                });

        // 创建并显示对话框
        Dialog dialog = builder.create();
        dialog.show();
        Button button_rename = dialogView.findViewById(R.id.button_rename);
        button_rename.setOnClickListener(v -> {
            // 重命名
//                Log.d(TAG,"重命名类别,type_id="+type_id);
            showRenameDialog(type_id, dialogView_fa);
        });


        Button button_del = dialogView.findViewById(R.id.button_del);
        button_del.setOnClickListener(v -> { // 删除类别
            // Log.d(TAG,"删除类别,type_id="+type_id);
            showConfirmDialog(type_id, dialogView_fa);

        });

    }

    private void showConfirmDialog(int type_id, View dialogView_fa){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确定要删除此类别吗？")
                .setMessage("仅删除类别，笔记会保留")
                .setCancelable(false)
                .setPositiveButton("删除", (dialog, id) -> {
                    // 用户点击了“Yes”按钮
                    vis_type = FileUtils.readIntArray(getApplicationContext(), "vis_type.txt");
                    vis_type[type_id]=0; // 记录为未启用
                    FileUtils.writeIntArray(getApplicationContext(), "vis_type.txt", vis_type);
//
                    type_name=readStrArray("type_name.txt");
                    type_name[type_id]=""; // 删除类别名称
                    writeStrArray("type_name.txt", type_name);
//
                    note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt"); //
                    for(int i=1;i<=max_num_notes;i++){
                        if(note_type_id[i]==type_id){
                            note_type_id[i]=0; // 恢复为无类别的标签
                        }
                    }
//                    writeIntArrayToFile("note_type_id.txt", note_type_id);
                    FileUtils.writeIntArray(getApplicationContext(), "note_type_id.txt", note_type_id);

                    if(type_show == type_id){ // 正在显示当前标签
                        type_show = -1; // 改为显示全部标签
                        update_main();
                    }
                    update_dialog(dialogView_fa);
                })
                .setNegativeButton("取消", (dialog, id) -> {
                    // 用户点击了“No”按钮
                    dialog.cancel();
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    private void showRenameDialog(int type_id, View dialogView_fa){
        // 使用 LayoutInflater 加载对话框的布局
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.rename, null);

        // 创建 AlertDialog.Builder 对象
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true) // 是否可以通过点击对话框外部取消
                .setPositiveButton("确定", (dialog, id2) -> {
                    // 用户点击了 Yes 按钮，执行操作
                    EditText editText = dialogView.findViewById(R.id.editText); //
                    String new_name = editText.getText().toString(); //

                    Log.d(TAG, "new_name = "+new_name);

                    if(new_name.length()==0){ //
                        //
                        Log.d(TAG, "类别名称不能为空");
                        Toast.makeText(MainActivity.this, "类别名称不能为空", Toast.LENGTH_SHORT).show();

                    }else{
                        type_name = readStrArray("type_name.txt");
                        type_name[type_id] = new_name; // 重命名
                        writeStrArray("type_name.txt", type_name);

                        update_dialog(dialogView_fa);
                        update_main();
                    }
                })
                .setNegativeButton("取消", (dialog, id) -> {
                    // 用户点击 Cancel 按钮后的操作
                    dialog.dismiss();
                });

        // 创建并显示对话框
        Dialog dialog = builder.create();
        dialog.show();

    }

    public void update_dialog(View dialogView){ // 更新类别弹窗
        LinearLayout layout = dialogView.findViewById(R.id.note_buttons_container);
        layout.removeAllViews(); // 清空

        vis_type = FileUtils.readIntArray(getApplicationContext(), "vis_type.txt");
        if(vis_type==null){
            Log.d(TAG,"vis_type==null"); //
        }else{
            for(int i=1;i<=max_num_types;i++){
                if(vis_type[i]==1){
                    addType(dialogView, i); // 添加按钮
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        textViewNoteType =  findViewById(R.id.textViewNoteType);
        if(type_show==-1){
            textViewNoteType.setText("全部笔记");
        }else if(type_show==0) {
            textViewNoteType.setText("未分类笔记");
        }else{
            textViewNoteType.setText(type_name[type_show]);
        }

        vis = FileUtils.readIntArray(getApplicationContext(), "vis.txt");


        titles = readStrArray("title.txt");
//        timeMillis = readLongArray("timeMillis.txt");
        timeMillis = FileUtils.readLongArray(getApplicationContext(), "timeMillis.txt");

        int c1=0;
        for(int i=1;i<=max_num_notes;i++){
            if(vis[i]==1){
                c1++;
            }
        }
        buttonCounter=c1;

        update_main();
    }
    private void update_main(){
//        Log.d(TAG,"update_main(), type_show="+type_show);

        type_name = readStrArray("type_name.txt");
        if(type_show==-1){
            textViewNoteType.setText("全部笔记");
        }else if(type_show==0) {
            textViewNoteType.setText("未分类笔记");
        }else{
            textViewNoteType.setText(type_name[type_show]);
        }

        LinearLayout layout = findViewById(R.id.note_buttons_container);
        layout.removeAllViews(); // 清空LinearLayout

//        note_type_id = readIntArrayFromFile("note_type_id.txt");
        note_type_id = FileUtils.readIntArray(getApplicationContext(), "note_type_id.txt");

        int c2=0;
        if(buttonCounter>=1 && buttonCounter<=1000){ //
            // 按照时间戳降序输出
            List<Note> notes = new ArrayList<>();

            for(int i=1;i<=max_num_notes;i++){
                if(vis[i]==1){
                    if(type_show==-1 || note_type_id[i]==type_show){ //
                        notes.add(new Note(timeMillis[i], i));
                        c2++;
                        //
//                        Log.d(TAG,"update_main(), i = "+i);
                    }
                }
            }
            Collections.sort(notes); // 按时间戳降序排序

            int first = 1;
            for (Note n1 : notes) {
                int id = n1.getId();
                if(first==1){ // 第一个
                    first=0;
                    addTag(id); // 添加按钮
                }else{
                    addLine(); // 添加分割行
                    addTag(id); // 添加按钮
                }
            }
        }
        myTextView = findViewById(R.id.textView);
        myTextView.setText(String.format(Locale.getDefault(), "%d篇笔记", c2));
    }

    private void addLine(){ // 添加分割行
        // 创建一个新的View对象
        View divider = new View(this);

        // 设置View的宽度为MATCH_PARENT（匹配父容器宽度），高度为具体值（例如1dp）
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics())
        );

        // 设置背景颜色为灰色
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_DD)); // 使用Android预定义的颜色，或者你可以使用R.color.your_custom_gray_color

        // 将divider添加到你的布局中（假设你有一个LinearLayout布局）
        noteButtonsContainer.addView(divider, params);
    }

    private void addTag(int id) { // 添加序号为id的便签
        if(id<=0)return;

        // Create a new button
        Button noteButton = new Button(this);
        noteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));

        noteButton.setTransformationMethod(null); // 禁用文本大写转换

        noteButton.setText(titles[id]);
        noteButton.setTextSize(18);
        noteButton.setTypeface(null, Typeface.BOLD);

        noteButton.setLayoutParams(new LinearLayout.LayoutParams( // 设置按钮的布局参数
                LinearLayout.LayoutParams.MATCH_PARENT, // 宽度
                300 // 高度
//                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Set an OnClickListener for the new button
        noteButton.setOnClickListener(v -> { // 进入便签

            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("globalVariable", String.valueOf(id));
            editor.apply();

            // Open the NoteActivity
            Intent intent = new Intent(MainActivity.this, NewActivity.class);
            // Optionally, you can pass some data to NoteActivity using intent.putExtra()
            startActivity(intent);
        });

        // Add the new button to the container
        noteButtonsContainer.addView(noteButton);
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