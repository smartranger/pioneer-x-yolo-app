package com.tencent.yolov8ncnn;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class AppInfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_info);
        
        // 隐藏ActionBar
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        
        // 设置系统UI，只隐藏导航栏，保留状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // 从配置文件获取应用说明文本
        TextView textViewAppInfo = findViewById(R.id.textViewAppInfo);
        textViewAppInfo.setText(getString(R.string.app_info_text));
        
        // 找到关闭按钮并设置点击事件
        Button buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前Activity
            }
        });
    }
} 