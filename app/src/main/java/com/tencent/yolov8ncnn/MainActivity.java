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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;

    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private int facing = 1; // 0: front, 1: back

    // 调试模式标志，控制调试面板是否显示
    private static final boolean DEBUG_PANEL_VISIBLE = false; // 设置为false可隐藏调试面板

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 1; // 0: yolov8n, 1: yolov8s
    private int current_cpugpu = 0;
    
    // 添加UI控制选项
    private boolean ui_enabled = false; // 默认启用UI
    private android.widget.Switch switchUIVisible;
    
    // 添加语言控制选项
    private Spinner spinnerLanguage;
    private int current_language = 0; // 默认使用中文
    
    // 添加目标标签选择器
    private Spinner spinnerTargetLabel;
    private int current_target_label = 0; // 默认值，将从配置文件获取
    
    // 中文标签数组
    private static final String[] CHINESE_LABELS = {
        "人", "自行车", "汽车", "摩托车", "飞机", "公交车", "火车", "卡车", "船", "交通灯",
        "消防栓", "停止标志", "停车计时器", "长凳", "鸟", "猫", "狗", "马", "羊", "牛",
        "大象", "熊", "斑马", "长颈鹿", "背包", "雨伞", "手提包", "领带", "行李箱", "飞盘",
        "滑雪板", "单板滑雪", "运动球", "风筝", "棒球棒", "棒球手套", "滑板", "冲浪板",
        "网球拍", "瓶子", "酒杯", "杯子", "叉子", "刀", "勺子", "碗", "香蕉", "苹果",
        "三明治", "橙子", "西兰花", "胡萝卜", "热狗", "披萨", "甜甜圈", "蛋糕", "椅子", "沙发",
        "盆栽植物", "床", "餐桌", "厕所", "电视", "笔记本电脑", "鼠标", "遥控器", "键盘", "手机",
        "微波炉", "烤箱", "烤面包机", "水槽", "冰箱", "书", "时钟", "花瓶", "剪刀", "泰迪熊",
        "吹风机", "牙刷"
    };
    
    // 英文标签数组
    private static final String[] ENGLISH_LABELS = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    };
    
    // 检测到的对象列表
    private Yolov8Ncnn.DetectedObject[] detectedObjects;
    
    // 检测结果覆盖视图
    private DetectionOverlayView detectionOverlay;
    
    // 检测结果监听器
    private Yolov8Ncnn.DetectionListener detectionListener = new Yolov8Ncnn.DetectionListener() {
        @Override
        public void onObjectsDetected(Yolov8Ncnn.DetectedObject[] objects) {
            detectedObjects = objects;
            
            // 更新覆盖视图显示检测结果
            if (detectionOverlay != null) {
                // 筛选检测结果
                Yolov8Ncnn.DetectedObject[] filteredObjects = getHighestConfidenceObject(objects);
                detectionOverlay.setDetectedObjects(filteredObjects);
                
                // 如果有对象，且第一个对象包含帧尺寸信息，则更新imageWidth和imageHeight
                if (objects != null && objects.length > 0 && objects[0].frameWidth > 0 && objects[0].frameHeight > 0) {
                    imageWidth = objects[0].frameWidth;
                    imageHeight = objects[0].frameHeight;
                    detectionOverlay.setImageSize(imageWidth, imageHeight);
                }
                
                // 如果处于检测状态，检查是否匹配目标标签
                if (currentState == AppState.DETECTING && filteredObjects != null && filteredObjects.length > 0) {
                    checkTargetLabelMatch(filteredObjects);
                } else if (currentState == AppState.DETECTING) {
                    // 没有检测到物体
                    updateDetectionStatus(false, "未检测到物体");
                }
            }
        }
    };

    // 筛选出检测结果
    private Yolov8Ncnn.DetectedObject[] getHighestConfidenceObject(Yolov8Ncnn.DetectedObject[] objects) {
        if (objects == null || objects.length == 0) {
            return null;
        }
        
        // 收集所有目标物体
        java.util.List<Yolov8Ncnn.DetectedObject> targetObjects = new java.util.ArrayList<>();
        
        // 同时记录置信度最高的物体（用于没有目标物体时）
        Yolov8Ncnn.DetectedObject highestObject = objects[0];
        
        for (Yolov8Ncnn.DetectedObject obj : objects) {
            // 更新置信度最高的物体
            if (obj.prob > highestObject.prob) {
                highestObject = obj;
            }
            
            // 如果是目标标签物体，添加到列表中
            if (obj.label == current_target_label) {
                targetObjects.add(obj);
            }
        }
        
        // 如果找到了目标物体，返回所有目标物体
        if (!targetObjects.isEmpty()) {
            Yolov8Ncnn.DetectedObject[] result = new Yolov8Ncnn.DetectedObject[targetObjects.size()];
            return targetObjects.toArray(result);
        }
        
        // 否则返回置信度最高的物体
        return new Yolov8Ncnn.DetectedObject[] { highestObject };
    }
    
    // 检查是否匹配目标标签
    private void checkTargetLabelMatch(Yolov8Ncnn.DetectedObject[] detectedObjects) {
        // 确保有检测到物体
        if (detectedObjects == null || detectedObjects.length == 0) {
            updateDetectionStatus(false, "未检测到物体");
            return;
        }
        
        // 检查是否全部都是目标标签
        boolean allAreTargetLabel = true;
        for (Yolov8Ncnn.DetectedObject obj : detectedObjects) {
            if (obj.label != current_target_label) {
                allAreTargetLabel = false;
                break;
            }
        }
        
        int targetLabelIndex = current_target_label;
        String targetLabel = getLabelText(targetLabelIndex);
        
        if (allAreTargetLabel) {
            // 全部是目标标签
            // updateDetectionStatus(true, "正确: 检测到" + detectedObjects.length + "个" + targetLabel);
            updateDetectionStatus(true, "已识别");
        } else {
            // 如果只有一个物体且不是目标标签
            if (detectedObjects.length == 1) {
                String detectedLabel = getLabelText(detectedObjects[0].label);
                // updateDetectionStatus(false, "错误: 目标是" + targetLabel + "，检测到" + detectedLabel);
                updateDetectionStatus(false, getString(R.string.prompt_message2));
            } else {
                // 多个物体中有非目标标签
                // updateDetectionStatus(false, "错误: 存在非目标物体");
                updateDetectionStatus(false, getString(R.string.prompt_message2));
            }
        }
    }
    
    // 更新检测状态提示
    private void updateDetectionStatus(boolean isCorrect, String message) {
        if (statusText != null) {
            // 显示识别结果
            statusText.setText(message);
            statusText.setTextColor(getResources().getColor(
                    isCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light));
        }
        
        // 如果处于检测状态，更新按钮图标
        if (currentState == AppState.DETECTING && buttonStartCamera != null) {
            ((ImageButton)buttonStartCamera).setImageResource(
                    isCorrect ? R.drawable.btn_detecting_correct : R.drawable.btn_detecting_wrong);
        }
        
        // 更新检测框颜色
        if (detectionOverlay != null) {
            detectionOverlay.setDetectionCorrect(isCorrect);
        }
        
        // 如果检测正确，播放提示音
        if (isCorrect) {
            playDetectionSound();
        }
    }

    // 通用音频加载方法，支持多种格式
    private boolean loadAudioFromAssets(MediaPlayer player) {
        // 支持的音频格式
        String[] supportedFormats = {".mp3", ".wav", ".ogg", ".m4a"};
        
        try {
            AssetManager assetManager = getAssets();
            AssetFileDescriptor afd = null;
            
            // 尝试不同的音频格式
            for (String format : supportedFormats) {
                try {
                    String fullFileName = "res/detection_sound" + format;
                    afd = assetManager.openFd(fullFileName);
                    Log.i("MainActivity", "成功找到音频文件: " + fullFileName);
                    break;
                } catch (Exception e) {
                    Log.d("MainActivity", "音频文件不存在: res/detection_sound" + format);
                    continue;
                }
            }
            
            if (afd == null) {
                Log.e("MainActivity", "未找到音频文件: detection_sound (尝试了所有支持的格式)");
                return false;
            }
            
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            afd.close();
            player.setVolume(1.0f, 1.0f);
            
            Log.i("MainActivity", "音频文件加载成功: detection_sound");
            return true;
            
        } catch (Exception e) {
            Log.e("MainActivity", "加载音频文件失败: detection_sound" + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    // 创建并初始化MediaPlayer
    private MediaPlayer createMediaPlayer() {
        try {
            MediaPlayer player = new MediaPlayer();
            
            if (loadAudioFromAssets(player)) {
                return player;
            } else {
                player.release();
                return null;
            }
        } catch (Exception e) {
            Log.e("MainActivity", "创建 MediaPlayer 失败: " + e.getMessage());
            return null;
        }
    }

    // 播放检测提示音
    private void playDetectionSound() {
        if (mediaPlayer == null) {
            Log.e("MainActivity", "MediaPlayer 为空，尝试重新创建");
            mediaPlayer = createMediaPlayer();
            if (mediaPlayer == null) {
                Log.e("MainActivity", "重新创建 MediaPlayer 失败");
                return;
            }
        }
        
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            
            if (!loadAudioFromAssets(mediaPlayer)) {
                Log.e("MainActivity", "重新加载音频文件失败");
                return;
            }
            
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.i("MainActivity", "音频播放完成");
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("MainActivity", "音频播放错误: what=" + what + ", extra=" + extra);
                    return false;
                }
            });
            mediaPlayer.start();
            Log.i("MainActivity", "开始播放音频");
        } catch (Exception e) {
            Log.e("MainActivity", "播放音频失败: " + e.getMessage());
        }
    }

    private SurfaceView cameraView;
    private TextView statusText;
    private ImageButton buttonStartCamera;
    private ImageView initialImage;
    
    // 应用状态枚举
    private enum AppState {
        INITIAL,        // 初始状态：相机不显示，等待用户点击"开始识别"
        READY_TO_DETECT, // 相机已打开，等待用户点击"识别"按钮
        DETECTING       // 正在检测物体
    }
    
    private AppState currentState = AppState.INITIAL;
    private boolean isCameraOpen = false; // 相机状态标志

    // 添加原始图像尺寸参数
    private int imageWidth = 640;  // 默认值，应根据实际相机预览尺寸设置
    private int imageHeight = 480; // 默认值，应根据实际相机预览尺寸设置

    private MediaPlayer mediaPlayer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // 从配置文件获取默认目标标签
        current_target_label = getResources().getInteger(R.integer.default_target_label);
        
        // 初始化 MediaPlayer
        mediaPlayer = createMediaPlayer();
        if (mediaPlayer == null) {
            Log.e("MainActivity", "创建 MediaPlayer 失败");
        } else {
            Log.i("MainActivity", "MediaPlayer 初始化成功");
        }
        
        // 隐藏ActionBar
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        
        // 设置系统UI，只隐藏导航栏，保留状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                      | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // 根据调试标志控制调试面板的显示
        View debugPanel = findViewById(R.id.debugPanel);
        if (debugPanel != null) {
            debugPanel.setVisibility(DEBUG_PANEL_VISIBLE ? View.VISIBLE : View.GONE);
        }

        cameraView = (SurfaceView) findViewById(R.id.cameraview);
        statusText = (TextView) findViewById(R.id.statusText);
        buttonStartCamera = (ImageButton) findViewById(R.id.buttonStartCamera);
        initialImage = (ImageView) findViewById(R.id.initialImage);
        detectionOverlay = (DetectionOverlayView) findViewById(R.id.detectionOverlay);
        
        // 初始化UI控制选项
        switchUIVisible = (android.widget.Switch) findViewById(R.id.switchUIVisible);
        spinnerLanguage = (Spinner) findViewById(R.id.spinnerLanguage);
        spinnerTargetLabel = (Spinner) findViewById(R.id.spinnerTargetLabel);
        
        // 设置UI可见性开关的监听器
        if (switchUIVisible != null) {
            switchUIVisible.setChecked(ui_enabled);
            switchUIVisible.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    ui_enabled = isChecked;
                    updateUIOptions();
                }
            });
        }
        
        // 设置语言选择器的监听器
        if (spinnerLanguage != null) {
            spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != current_language) {
                        current_language = position;
                        updateLanguageOption();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        
        // 设置目标标签选择器
        if (spinnerTargetLabel != null) {
            // 设置适配器
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, CHINESE_LABELS);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTargetLabel.setAdapter(adapter);
            
            // 设置监听器
            spinnerTargetLabel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    current_target_label = position;
                    // 更新选择的目标标签
                    Log.i("MainActivity", "目标标签已设置为: " + getLabelText(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);
        
        // 设置初始状态
        updateAppState(AppState.INITIAL);

        // 加载初始图片，优先使用本地资源，失败则尝试网络加载
        loadInitialImage();

        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!isCameraOpen) return; // 如果相机未打开，不执行切换

                int new_facing = 1 - facing;

                yolov8ncnn.closeCamera();
                yolov8ncnn.openCamera(new_facing);
                
                // 更新水平翻转设置
                if (detectionOverlay != null) {
                    detectionOverlay.setFlipHorizontal(new_facing == 0);
                }
                
                facing = new_facing;
            }
        });

        // 添加开始识别按钮的点击事件
        buttonStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currentState) {
                    case INITIAL:
                        // 第一次点击"开始识别"按钮，打开相机并进入READY_TO_DETECT状态
                        if (checkCameraPermission()) {
                            // 有权限，打开相机
                            cameraView.setVisibility(View.VISIBLE);
                            yolov8ncnn.openCamera(facing);
                            isCameraOpen = true;
                            updateAppState(AppState.READY_TO_DETECT);
                        } else {
                            // 没有权限，显示提示并询问是否跳转设置
                            Log.e("MainActivity", "Camera permission not granted");
                            
                            // 查询是否已经被永久拒绝
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                                // 用户只是暂时拒绝，可以再次请求
                                android.widget.Toast.makeText(MainActivity.this, 
                                        "需要相机权限才能打开相机", 
                                        android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                // 用户选择了"不再询问"，需要引导用户手动开启权限
                                showSettingsDialog();
                            }
                        }
                        break;
                        
                    case READY_TO_DETECT:
                        // 点击"识别"按钮，进入DETECTING状态，对当前画面进行物体检测
                        updateAppState(AppState.DETECTING);
                        break;
                        
                    case DETECTING:
                        // 如果正在检测中，点击按钮停止检测并返回到READY_TO_DETECT状态
                        updateAppState(AppState.READY_TO_DETECT);
                        break;
                }
            }
        });
        
        // 添加App说明按钮的点击事件
        Button buttonAppInfo = (Button) findViewById(R.id.buttonAppInfo);
        buttonAppInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到App说明页面
                Intent intent = new Intent(MainActivity.this, AppInfoActivity.class);
                startActivity(intent);
            }
        });

        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_model)
                {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_cpugpu)
                {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        reload();
        
        // 初始化UI选项
        updateUIOptions();
        updateLanguageOption();

        // 设置检测结果监听器
        yolov8ncnn.setDetectionListener(detectionListener);

        // 初始化覆盖视图
        if (detectionOverlay != null) {
            // 设置标签数组
            detectionOverlay.setLabels(current_language == 0 ? CHINESE_LABELS : ENGLISH_LABELS);
        }

        // 初始化YOLO模型
        yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        
        // 启用中文标签显示（0=中文，1=英文）
        yolov8ncnn.setLanguage(0);
        
        // 设置UI选项，关闭自动检测，只在用户点击识别按钮时检测
        yolov8ncnn.setUIOptions(false);
    }
    
    // 更新应用状态的方法
    private void updateAppState(AppState newState) {
        currentState = newState;
        
        switch (newState) {
            case INITIAL:
                // 初始状态：SurfaceView不显示，显示初始图片，状态文字为空，按钮为GO图标
                cameraView.setVisibility(View.GONE);
                detectionOverlay.setVisibility(View.GONE);
                initialImage.setVisibility(View.VISIBLE);
                statusText.setText("");
                ((ImageButton)buttonStartCamera).setImageResource(R.drawable.btn_initial);
                
                // 确保检测功能关闭
                yolov8ncnn.setUIOptions(false);
                break;
                
            case READY_TO_DETECT:
                // 相机已打开状态：SurfaceView显示，隐藏初始图片，状态文字提示，按钮为相机图标
                cameraView.setVisibility(View.VISIBLE);
                // 在READY_TO_DETECT状态下隐藏检测覆盖层
                detectionOverlay.setVisibility(View.GONE);
                initialImage.setVisibility(View.GONE);
                statusText.setText("点击按钮开始识别");
                statusText.setTextColor(getResources().getColor(android.R.color.black));
                ((ImageButton)buttonStartCamera).setImageResource(R.drawable.btn_ready_to_detect);
                
                // 关闭持续识别模式 - 这是关键部分，设置为false表示不自动检测
                yolov8ncnn.setUIOptions(false);
                
                // 清空覆盖视图中的检测结果
                detectionOverlay.setDetectedObjects(null);
                // 重置为默认颜色（蓝色）
                detectionOverlay.resetToDefaultColor();
                
                // 恢复相机预览
                yolov8ncnn.resumeCameraPreview();
                break;
                
            case DETECTING:
                // 正在检测状态：按钮根据检测结果显示对应图标
                statusText.setText("正在识别...");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                
                // 暂时使用默认图标，会在检测结果回调中更新
                ((ImageButton)buttonStartCamera).setImageResource(R.drawable.btn_detecting_wrong);
                
                // 显示检测覆盖层
                detectionOverlay.setVisibility(View.VISIBLE);
                // 重置为默认颜色（蓝色）
                detectionOverlay.resetToDefaultColor();
                
                // 暂停相机预览以使画面静止
                yolov8ncnn.pauseCameraPreview();
                
                // 对当前帧进行一次检测，不启用持续检测模式
                // 重要：保持检测功能为关闭状态，仅执行单次检测
                yolov8ncnn.detectCurrentFrame();
                break;
        }
    }

    private void reload()
    {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (isCameraOpen) {
            yolov8ncnn.setOutputWindow(holder.getSurface());
            
            // 获取摄像头帧尺寸
            int[] frameSize = yolov8ncnn.getFrameSize();
            if (frameSize != null && frameSize.length == 2) {
                imageWidth = frameSize[0];
                imageHeight = frameSize[1];
                
                // 更新绘制组件的图像尺寸
                if (detectionOverlay != null) {
                    detectionOverlay.setImageSize(imageWidth, imageHeight);
                    detectionOverlay.setFlipHorizontal(facing == 0); // 前置摄像头需要水平翻转
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // 如果之前相机是开着的，恢复相机状态
        if (isCameraOpen) {
            yolov8ncnn.openCamera(facing);
            
            // 如果之前是在DETECTING状态，恢复到READY_TO_DETECT状态
            if (currentState == AppState.DETECTING) {
                updateAppState(AppState.READY_TO_DETECT);
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // 关闭相机，但保持状态标记不变
        if (isCameraOpen) {
            yolov8ncnn.closeCamera();
            
            // 如果正在检测，保存当前状态便于恢复
            // 实际状态切换在onResume中处理
        }
    }
    
    // 检查权限的通用方法
    private boolean checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) 
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(this, 
                    new String[] {permission}, requestCode);
            return false;
        }
        return true;
    }
    
    // 检查相机权限
    private boolean checkCameraPermission() {
        return checkPermission(Manifest.permission.CAMERA, REQUEST_CAMERA);
    }

    // 显示引导用户前往设置页面的对话框
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("权限被拒绝")
               .setMessage("没有相机权限，无法打开相机。您可以到设置中手动开启权限。")
               .setPositiveButton("去设置", new android.content.DialogInterface.OnClickListener() {
                   public void onClick(android.content.DialogInterface dialog, int id) {
                       openAppSettings();
                   }
               })
               .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                   public void onClick(android.content.DialogInterface dialog, int id) {
                       dialog.dismiss();
                   }
               });
        builder.create().show();
    }

    // 修改权限结果处理方法
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "Camera permission granted");
                
                // 权限获取成功，更新UI并打开相机
                cameraView.setVisibility(View.VISIBLE);
                yolov8ncnn.openCamera(facing);
                isCameraOpen = true;
                updateAppState(AppState.READY_TO_DETECT);
                
                android.widget.Toast.makeText(this, 
                        "相机权限已获取", 
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                Log.e("MainActivity", "Camera permission denied");
                
                // 如果权限被永久拒绝，显示设置对话框
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showSettingsDialog();
                } else {
                    android.widget.Toast.makeText(this, 
                            "相机权限被拒绝，请重试", 
                            android.widget.Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    // 打开应用设置页面
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    // 加载初始图片的方法
    private void loadInitialImage() {
        try {
            // 尝试加载本地图片资源
            initialImage.setImageResource(R.drawable.img_surface_bg);
        } catch (Exception e) {
            Log.e("MainActivity", "加载图片失败: " + e.getMessage());
            initialImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    // 更新UI选项的方法
    private void updateUIOptions() {
        if (yolov8ncnn != null) {
            // 只设置UI是否显示，不再设置样式
            yolov8ncnn.setUIOptions(ui_enabled);
        }
    }
    
    // 更新语言选项的方法
    private void updateLanguageOption() {
        yolov8ncnn.setLanguage(current_language);
        
        // 同时更新覆盖视图的标签数组
        if (detectionOverlay != null) {
            detectionOverlay.setLabels(current_language == 0 ? CHINESE_LABELS : ENGLISH_LABELS);
        }
        
        // 更新目标标签下拉列表
        if (spinnerTargetLabel != null) {
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, 
                    current_language == 0 ? CHINESE_LABELS : ENGLISH_LABELS);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTargetLabel.setAdapter(adapter);
            spinnerTargetLabel.setSelection(current_target_label);
        }
    }

    // 获取当前语言下的标签文本
    private String getLabelText(int labelIndex) {
        if (labelIndex < 0 || labelIndex >= CHINESE_LABELS.length) {
            return "unknown";
        }
        
        return current_language == 0 ? CHINESE_LABELS[labelIndex] : ENGLISH_LABELS[labelIndex];
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放 MediaPlayer 资源
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.i("MainActivity", "MediaPlayer 资源已释放");
            } catch (Exception e) {
                Log.e("MainActivity", "释放 MediaPlayer 资源失败: " + e.getMessage());
            }
        }
    }
}
