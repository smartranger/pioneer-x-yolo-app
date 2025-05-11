// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov8ncnn;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class SplashActivity extends Activity {
    
    private static final String TAG = "SplashActivity";
    private RelativeLayout mOverlayContainer;
    private ImageView mSplashImageView;
    private String SPLASH_IMAGE_PATH;
    private int DELAY_TIME;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 从配置文件中获取参数
        SPLASH_IMAGE_PATH = getString(R.string.splash_image_path);
        DELAY_TIME = getResources().getInteger(R.integer.splash_delay_time);
        
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.splash);
        
        // 获取控件引用
        mOverlayContainer = findViewById(R.id.overlay_container);
        mSplashImageView = findViewById(R.id.splash_image_main);
        
        // 加载指定的启动图片
        loadSplashImage();
        
        // 3秒后显示渐变背景和文字
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showOverlay();
            }
        }, DELAY_TIME);
        
        // 设置点击任意处进入MainActivity
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMainActivity();
            }
        });
    }
    
    /**
     * 从assets指定位置加载启动图片
     */
    private void loadSplashImage() {
        try {
            // 从assets/images/目录加载启动图片
            InputStream is = getAssets().open(SPLASH_IMAGE_PATH);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            
            if (bitmap != null) {
                // 设置图片到ImageView
                mSplashImageView.setImageBitmap(bitmap);
                Log.d(TAG, "成功加载启动图片: " + SPLASH_IMAGE_PATH);
            } else {
                Log.e(TAG, "图片解码失败");
                Toast.makeText(this, "无法加载启动图片", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "加载启动图片失败: " + e.getMessage());
            Toast.makeText(this, "加载启动图片出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // 显示渐变背景和文字
    private void showOverlay() {
        // 创建淡入动画
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500); // 淡入动画持续500毫秒
        
        // 设置动画结束后的状态
        fadeIn.setFillAfter(true);
        
        // 显示叠加层并开始动画
        mOverlayContainer.setVisibility(View.VISIBLE);
        mOverlayContainer.startAnimation(fadeIn);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // 结束当前Activity，防止返回键返回到启动页
    }
} 