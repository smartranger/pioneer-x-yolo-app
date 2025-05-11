package com.pioneer.appbuilder;

/**
 * 应用配置类
 * 用于保存与APK构建相关的所有配置参数
 */
public class AppConfig {
    // 基本应用信息
    public String appName = "ncnn-yolov8";
    public String packageName = "com.havingstar.wxapp.pioneer.x.yolo";
    public String versionName = "1.0.0";
    public int versionCode = 1;
    
    // 应用图标路径
    public String iconPath = "";
    
    // 签名配置
    public KeystoreConfig keystoreConfig = new KeystoreConfig();
    
    // 启动页配置
    public SplashConfig splashConfig = new SplashConfig();
    
    // 声音配置
    public SoundConfig soundConfig = new SoundConfig();
    
    // 应用信息配置
    public AppInfoConfig appInfoConfig = new AppInfoConfig();
    
    // 主界面配置
    public MainConfig mainConfig = new MainConfig();
    
    /**
     * Keystore签名配置
     */
    public static class KeystoreConfig {
        public String keystorePath = "app.keystore";
        public String keystorePassword = ""; // 请在运行时设置密码，不要使用默认值
        public String keyAlias = "key0";
        public String keyPassword = ""; // 请在运行时设置密码，不要使用默认值
    }
    
    /**
     * 启动页配置
     */
    public static class SplashConfig {
        public String imagePath = "images/splash.jpg";
        public int delayTime = 3000;
    }
    
    /**
     * 声音配置
     */
    public static class SoundConfig {
        public String detectionSoundResource = "detection_sound";
    }
    
    /**
     * 应用信息配置
     */
    public static class AppInfoConfig {
        public String infoText = "本应用是基于YOLO v8的目标检测应用。使用方法：启动相机，对准物体即可检测。系统支持多种目标类别识别。";
    }
    
    /**
     * 主界面配置
     */
    public static class MainConfig {
        public int defaultTargetLabel = 0;
    }
} 