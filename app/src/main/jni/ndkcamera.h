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

#ifndef NDKCAMERA_H
#define NDKCAMERA_H

#include <android/looper.h>
#include <android/native_window.h>
#include <android/sensor.h>
#include <camera/NdkCameraDevice.h>
#include <camera/NdkCameraManager.h>
#include <camera/NdkCameraMetadata.h>
#include <media/NdkImageReader.h>

#include <opencv2/core/core.hpp>

class NdkCamera
{
public:
    NdkCamera();
    virtual ~NdkCamera();

    // facing 0=front 1=back
    int open(int camera_facing = 0);
    void close();

    virtual void on_image(const cv::Mat& rgb) const;

    virtual void on_image(const unsigned char* nv21, int nv21_width, int nv21_height) const;

public:
    int camera_facing;
    int camera_orientation;

private:
    ACameraManager* camera_manager;
    ACameraDevice* camera_device;
    AImageReader* image_reader;
    ANativeWindow* image_reader_surface;
    ACameraOutputTarget* image_reader_target;
    ACaptureRequest* capture_request;
    ACaptureSessionOutputContainer* capture_session_output_container;
    ACaptureSessionOutput* capture_session_output;
    ACameraCaptureSession* capture_session;
};

class NdkCameraWindow : public NdkCamera
{
public:
    NdkCameraWindow();
    virtual ~NdkCameraWindow();

    void set_window(ANativeWindow* win);

    virtual void on_image_render(cv::Mat& rgb) const;

    virtual void on_image(const unsigned char* nv21, int nv21_width, int nv21_height) const;

    // 获取相机宽度
    int get_width() const;
    
    // 获取相机高度
    int get_height() const;
    
    // 暂停相机预览
    void pause_camera();
    
    // 恢复相机预览
    void resume_camera();
    
    // 请求单次捕获和处理
    void request_capture();

public:
    mutable int accelerometer_orientation;
    
    // 添加渲染宽度和高度为成员变量
    mutable int render_w;
    mutable int render_h;

private:
    ASensorManager* sensor_manager;
    mutable ASensorEventQueue* sensor_event_queue;
    const ASensor* accelerometer_sensor;
    ANativeWindow* win;
    
    // 添加相机预览状态标志
    bool is_preview_paused;
    
    // 添加单次捕获请求标志
    mutable bool request_capture_flag;
};

#endif // NDKCAMERA_H
