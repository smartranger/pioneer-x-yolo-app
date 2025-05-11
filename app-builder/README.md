# Pioneer X App 构建器

这是一个用于定制化构建 YOLO v8 ncnn Android应用的工具。该工具允许您配置应用的基本信息、资源和签名，然后生成可安装的APK文件。

## 功能

- 配置应用基本信息（应用名称、包名、版本）
- 自定义config.xml配置文件内容
- 更换应用图标
- 配置签名信息
- 生成带签名的APK包或不签名的APK包
- 自动适配Java版本（支持Java 8和Java 11+）

## 使用方法

### 前提条件

- JDK 8或更高版本（推荐使用JDK 11或更高版本）
- Android SDK
- Gradle（可选，如果系统中没有安装，工具会使用项目自带的gradlew）

### Java版本兼容性

该工具自动检测系统中的Java版本，并根据不同版本选择合适的Gradle插件版本：
- 对于Java 8环境：使用兼容的Android Gradle插件 4.1.3
- 对于Java 11+环境：使用较新的Android Gradle插件 7.0.4

### 构建

1. 克隆项目到本地
2. 进入项目根目录
3. 执行以下命令构建：

```bash
./gradlew build
```

### 运行

```bash
./gradlew run
```

或者直接运行生成的JAR文件：

```bash
java -jar build/libs/app-builder.jar
```

## 配置项说明

### 应用基本信息

- **应用名称**：显示在手机上的应用名称
- **包名**：应用的唯一标识符，如com.example.myapp
- **版本名称**：显示给用户的版本号，如1.0.0
- **版本号**：内部版本号，用于版本控制

### config.xml配置

- **启动页配置**
  - 启动页图片路径
  - 启动页延迟时间(毫秒)
  
- **声音配置**
  - 检测声音资源

- **App说明页面配置**
  - 应用说明文本

- **主界面配置**
  - 默认目标标签

### 应用图标

推荐使用512x512像素的PNG图片，放置在 `resources/icons/` 目录下。

### 签名信息

- **Keystore文件路径**
- **Keystore密码**
- **Key别名**
- **Key密码**

如果没有Keystore，可以通过工具创建新的Keystore。

## 目录结构

- `resources/icons/` - 存放应用图标
- `resources/splash/` - 存放启动页图片
- `resources/sounds/` - 存放声音资源
- `resources/keystore/` - 存放签名密钥
- `output/` - 输出APK文件
- `project/` - 可以放置Android项目源文件（可选）

## 常见问题解决

### Java版本问题
如果出现Java版本不兼容的错误，工具会自动调整使用的Gradle插件版本。然而，某些情况下可能需要手动升级Java版本：
```
Dependency requires at least JVM runtime version 11.
```
解决方法：安装并使用JDK 11或更高版本。

### 找不到Android SDK
如果出现找不到Android SDK的错误，请确保已正确设置ANDROID_HOME环境变量。

### Gradle构建失败
如果Gradle构建失败，请检查详细的构建日志，通常日志中会包含具体的错误信息。

## 注意事项

- 确保Android SDK环境变量已正确设置
- 修改包名后，可能需要更新相应的Java包结构
- 应用图标需要是合适大小的PNG格式图片
- 构建签名APK前，请确保签名密钥配置正确 