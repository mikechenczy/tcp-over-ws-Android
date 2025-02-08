package com.mj.tcpoverws;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends AppCompatActivity {

    public static MainActivity INSTANCE;

    public SharedPreferences sharedPreferences;

    private EditText configEditor;
    private TextView logView;
    private Button startButton, stopButton, saveButton, clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        INSTANCE = this;
        sharedPreferences = getSharedPreferences("yan", MODE_PRIVATE);

        configEditor = findViewById(R.id.configEditor);
        logView = findViewById(R.id.logView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        saveButton = findViewById(R.id.saveButton);
        clearButton = findViewById(R.id.clearButton);

        loadConfig();

        startButton.setOnClickListener(v -> startService());
        stopButton.setOnClickListener(v -> stopService());

        // 保存配置
        saveButton.setOnClickListener(v -> saveConfig());
        clearButton.setOnClickListener(v -> logView.setText(""));


        // 注册接收广播
        IntentFilter filter = new IntentFilter("com.mj.tcpoverws.UPDATE_LOG");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(updateReceiver, filter);
        }
    }

    // BroadcastReceiver 用于接收服务的广播并更新界面
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            log(message);
        }
    };

    private void saveConfig() {
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("config", configEditor.getText().toString());
        editor.apply();
    }

    // 读取 & 加载配置文件
    private void loadConfig() {
        configEditor.setText(sharedPreferences.getString("config", ""));
    }

    // 启动服务
    private void startService() {
        saveConfig();
        String config = configEditor.getText().toString();
        String[] args = config.split(" ");
        if(args.length>=2 && "auto".equals(args[1])) {
            try {
                URI url = new URI(args[0]);
                String hostname = url.getHost();
                if(!hostname.startsWith("[") && !hostname.matches("^\\d+.\\d+.\\d+.\\d+$")) {
                    config = "";
                    for(int i=0;i<args.length;i++) {
                        if(i==1) {
                            config += hostname;
                        } else {
                            config += args[i];
                        }
                        if(i!=args.length-1) {
                            config += " ";
                        }
                    }
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        Intent serviceIntent = new Intent(this, RunService.class);
        serviceIntent.putExtra("config", config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // 关闭所有 TCP 服务器
    private void stopService() {
        Intent serviceIntent = new Intent(this, RunService.class);
        stopService(serviceIntent);
        log("stopped!");
    }


    // 日志输出到 TextView
    public void log(String message) {
        runOnUiThread(() -> {
            logView.append(message + "\n");
            ((ScrollView) logView.getParent()).fullScroll(View.FOCUS_DOWN);
        });
    }
}
