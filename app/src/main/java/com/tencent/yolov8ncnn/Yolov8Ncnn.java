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

import android.content.res.AssetManager;
import android.view.Surface;

public class Yolov8Ncnn
{
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
    public native boolean openCamera(int facing);
    public native boolean closeCamera();
    public native boolean setOutputWindow(Surface surface);
    public native boolean setUIOptions(boolean showUI);
    public native boolean setLanguage(int languageID);
    
    // 添加暂停相机预览的方法
    public native boolean pauseCameraPreview();
    
    // 添加恢复相机预览的方法 
    public native boolean resumeCameraPreview();
    
    // 添加检测当前帧的方法
    public native boolean detectCurrentFrame();
    
    // 回调接口，用于获取检测结果
    public interface DetectionListener {
        void onObjectsDetected(DetectedObject[] objects);
    }
    
    // 设置检测结果监听器
    public native boolean setDetectionListener(DetectionListener listener);
    
    // 检测到的对象类
    public static class DetectedObject {
        public float x;       // 矩形左上角x坐标
        public float y;       // 矩形左上角y坐标
        public float width;   // 矩形宽度
        public float height;  // 矩形高度
        public int label;     // 标签索引
        public float prob;    // 置信度
        public int frameWidth;  // 当前画面总宽度
        public int frameHeight; // 当前画面总高度
        
        // 通过JNI创建对象的方法
        private static DetectedObject create(float x, float y, float width, float height, int label, float prob, int frameWidth, int frameHeight) {
            DetectedObject obj = new DetectedObject();
            obj.x = x;
            obj.y = y;
            obj.width = width;
            obj.height = height;
            obj.label = label;
            obj.prob = prob;
            obj.frameWidth = frameWidth;
            obj.frameHeight = frameHeight;
            return obj;
        }
    }

    // 获取当前帧尺寸的方法
    public native int[] getFrameSize();

    static {
        System.loadLibrary("yolov8ncnn");
    }
}
