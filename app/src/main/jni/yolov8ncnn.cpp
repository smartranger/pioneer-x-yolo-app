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

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>
#include <time.h>  // 添加time.h支持nanosleep函数

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

// 添加一个全局标志，控制是否允许检测
static bool g_detection_enabled = false;

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    // 不绘制FPS框
    // cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
    //                 cv::Scalar(255, 255, 255), -1);

    // cv::putText(rgb, text, cv::Point(x, y + label_size.height),
    //             cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolo* g_yolo = 0;
static ncnn::Mutex lock;

// 检测结果传递给Java的全局引用
static jobject g_detection_listener = 0;
static jmethodID g_method_on_objects_detected = 0;
static jclass g_detected_object_class = 0;
static jmethodID g_method_create_detected_object = 0;

// 添加全局JavaVM指针
static JavaVM* g_jvm = 0;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    // 只有当检测标志为true时才执行检测
    if (g_detection_enabled)
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo)
        {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects);

            g_yolo->draw(rgb, objects);
            
            // 移除调用Java方法渲染汉字的代码，让原生C++代码完成所有渲染
            // 仅保留检测结果回调部分
            if (g_detection_listener && g_jvm)
            {
                JNIEnv* env = 0;
                int status = 0;
                
                // 获取JNIEnv
                status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
                if (status == JNI_EDETACHED) {
                    // 附加当前线程到VM
                    if (g_jvm->AttachCurrentThread(&env, NULL) != JNI_OK) {
                        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to attach thread");
                        return;
                    }
                } else if (status != JNI_OK) {
                    __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to get JNIEnv");
                    return;
                }
                
                // 如果已设置监听器和相关方法
                if (g_method_on_objects_detected && g_detected_object_class && g_method_create_detected_object) {
                    // 创建Java对象数组
                    jobjectArray jObjArray = env->NewObjectArray(objects.size(), g_detected_object_class, NULL);
                    
                    // 当前帧尺寸
                    int frame_width = rgb.cols;
                    int frame_height = rgb.rows;
                    
                    // 填充对象数组
                    for (size_t i = 0; i < objects.size(); i++) {
                        const Object& obj = objects[i];
                        
                        // 使用Java方法创建Java对象
                        jobject jObj = env->CallStaticObjectMethod(g_detected_object_class, 
                                g_method_create_detected_object, 
                                (jfloat)obj.rect.x, 
                                (jfloat)obj.rect.y, 
                                (jfloat)obj.rect.width, 
                                (jfloat)obj.rect.height, 
                                (jint)obj.label, 
                                (jfloat)obj.prob,
                                (jint)frame_width,
                                (jint)frame_height);
                                
                        // 设置到数组
                        env->SetObjectArrayElement(jObjArray, i, jObj);
                        
                        // 释放局部引用
                        env->DeleteLocalRef(jObj);
                    }
                    
                    // 调用onObjectsDetected方法
                    env->CallVoidMethod(g_detection_listener, g_method_on_objects_detected, jObjArray);
                    
                    // 释放局部引用
                    env->DeleteLocalRef(jObjArray);
                }
                
                // 如果当前线程是动态附加的，释放它
                if (status == JNI_EDETACHED) {
                    g_jvm->DetachCurrentThread();
                }
            }
        }
    }

    draw_fps(rgb);
}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    // 存储JavaVM指针到全局变量
    g_jvm = vm;
    
    // 确保检测标志初始为禁用状态
    g_detection_enabled = false;

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
        "n",
        "s",
    };

    const int target_sizes[] =
    {
        320,
        320,
    };

    const float mean_vals[][3] =
    {
        {103.53f, 116.28f, 123.675f},
        {103.53f, 116.28f, 123.675f},
    };

    const float norm_vals[][3] =
    {
        { 1 / 255.f, 1 / 255.f, 1 / 255.f },
        { 1 / 255.f, 1 / 255.f, 1 / 255.f },
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        }
        else
        {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

// public native boolean setUIOptions(boolean showUI);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setUIOptions(JNIEnv* env, jobject thiz, jboolean showUI)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setUIOptions %d", showUI);

    // 更新检测启用标志
    g_detection_enabled = showUI;

    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo)
        {
            g_yolo->setUIOptions(showUI);
        }
    }

    return JNI_TRUE;
}

// public native boolean setLanguage(int languageID);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setLanguage(JNIEnv* env, jobject thiz, jint languageID)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setLanguage %d", languageID);

    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo)
        {
            g_yolo->setLanguage(languageID);
        }
    }

    return JNI_TRUE;
}

// public native boolean setDetectionListener(DetectionListener listener);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setDetectionListener(JNIEnv* env, jobject thiz, jobject listener)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setDetectionListener %p", listener);
    
    // 释放之前的全局引用
    if (g_detection_listener) {
        env->DeleteGlobalRef(g_detection_listener);
        g_detection_listener = 0;
    }
    
    // 如果参数不为null，创建全局引用并获取方法ID
    if (listener) {
        g_detection_listener = env->NewGlobalRef(listener);
        
        // 获取DetectionListener类
        jclass listenerClass = env->GetObjectClass(listener);
        if (!listenerClass) {
            __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to get DetectionListener class");
            return JNI_FALSE;
        }
        
        // 获取onObjectsDetected方法ID
        g_method_on_objects_detected = env->GetMethodID(
            listenerClass, 
            "onObjectsDetected", 
            "([Lcom/tencent/yolov8ncnn/Yolov8Ncnn$DetectedObject;)V"
        );
        if (!g_method_on_objects_detected) {
            __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to get onObjectsDetected method");
            return JNI_FALSE;
        }
        
        // 获取DetectedObject类
        jclass objClass = env->FindClass("com/tencent/yolov8ncnn/Yolov8Ncnn$DetectedObject");
        if (!objClass) {
            __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to find DetectedObject class");
            return JNI_FALSE;
        }
        g_detected_object_class = (jclass)env->NewGlobalRef(objClass);
        
        // 获取create方法
        g_method_create_detected_object = env->GetStaticMethodID(
            g_detected_object_class, 
            "create", 
            "(FFFFIFII)Lcom/tencent/yolov8ncnn/Yolov8Ncnn$DetectedObject;"
        );
        if (!g_method_create_detected_object) {
            __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to get create method");
            return JNI_FALSE;
        }
        
        // 释放局部引用
        env->DeleteLocalRef(listenerClass);
        env->DeleteLocalRef(objClass);
    } else {
        // 如果传入null，清理全局引用和方法ID
        g_method_on_objects_detected = 0;
        if (g_detected_object_class) {
            env->DeleteGlobalRef(g_detected_object_class);
            g_detected_object_class = 0;
        }
        g_method_create_detected_object = 0;
    }
    
    return JNI_TRUE;
}

// public native int[] getFrameSize();
JNIEXPORT jintArray JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getFrameSize(JNIEnv* env, jobject thiz)
{
    if (!g_camera)
        return NULL;
    
    int width = g_camera->get_width();
    int height = g_camera->get_height();
    
    jintArray result = env->NewIntArray(2);
    if (result == NULL) {
        return NULL; // 内存分配失败
    }
    
    jint fill[2];
    fill[0] = width;
    fill[1] = height;
    env->SetIntArrayRegion(result, 0, 2, fill);
    
    return result;
}

// 添加暂停相机预览的JNI实现
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_pauseCameraPreview(JNIEnv* env, jobject thiz)
{
    if (!g_camera)
        return JNI_FALSE;
    
    // 暂停相机预览
    g_camera->pause_camera();
    
    return JNI_TRUE;
}

// 添加恢复相机预览的JNI实现
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_resumeCameraPreview(JNIEnv* env, jobject thiz)
{
    if (!g_camera)
        return JNI_FALSE;
    
    // 恢复相机预览
    g_camera->resume_camera();
    
    return JNI_TRUE;
}

// 添加检测当前帧的JNI实现
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_detectCurrentFrame(JNIEnv* env, jobject thiz)
{
    if (!g_camera || !g_yolo)
        return JNI_FALSE;
    
    // 保存当前检测状态
    bool previous_detection_state = g_detection_enabled;
    
    // 临时开启检测
    g_detection_enabled = true;
    
    // 请求捕获和处理一帧
    g_camera->request_capture();
    
    // 等待短暂时间确保检测完成
    struct timespec ts;
    ts.tv_sec = 0;
    ts.tv_nsec = 100 * 1000000; // 100毫秒
    nanosleep(&ts, NULL);
    
    // 恢复之前的检测状态
    g_detection_enabled = previous_detection_state;
    
    return JNI_TRUE;
}

}
