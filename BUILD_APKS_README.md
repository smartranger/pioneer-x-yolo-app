# 批量构建APK脚本使用说明

## 功能说明

这个脚本可以自动为每个用户配置生成独立的APK文件。脚本会：

1. 遍历 `userInfos` 文件夹中的所有子文件夹
2. 将每个子文件夹中的4个资源文件替换到项目的对应位置
3. 构建**签名的**release版本APK
4. 重命名APK为 `[文件夹名].apk` 并保存到 `userInfos` 目录下

## 目录结构

```
项目根目录/
├── userInfos/                    # 用户配置目录
│   ├── 38/                      # 用户文件夹（示例）
│   │   ├── config.xml           # 配置文件
│   │   ├── detection_sound.wav  # 检测声音文件
│   │   ├── ic_launcher.png      # 应用图标
│   │   └── splash.png           # 启动图片
│   ├── 39/                      # 另一个用户文件夹
│   │   └── ... (同样的4个文件)
│   └── ...
├── app/                         # Android应用目录
│   ├── release-key.keystore     # 签名密钥文件
│   ├── build.gradle             # 包含签名配置的构建文件
│   └── proguard-rules.pro       # ProGuard混淆规则
├── build_multiple_apks.py       # Python版本脚本
├── build_multiple_apks.sh       # Shell版本脚本
└── BUILD_APKS_README.md         # 本说明文件
```

## 文件映射关系

脚本会将用户文件夹中的文件替换到以下位置：

| 源文件 | 目标位置 |
|--------|----------|
| `config.xml` | `app/src/main/res/values/config.xml` |
| `detection_sound.wav` | `app/src/main/assets/res/detection_sound.wav` |
| `ic_launcher.png` | `app/src/main/res/mipmap/ic_launcher.png` |
| `splash.png` | `app/src/main/assets/res/splash.png` |

## 使用方法

### 方法1：使用Python脚本（推荐）

```bash
# 在项目根目录下运行
python3 build_multiple_apks.py
```

### 方法2：使用Shell脚本

```bash
# 在项目根目录下运行
./build_multiple_apks.sh
```

## 前置要求

1. **Android开发环境**：确保已安装Android SDK和配置好环境
2. **Gradle**：项目根目录下有可执行的 `gradlew` 脚本
3. **Python 3**（如果使用Python脚本）：系统需要安装Python 3.6+
4. **签名配置**：✅ 已自动配置完成，无需手动设置

## APK签名配置

项目已经配置了完整的签名设置：

### 自动配置的组件
- ✅ **Keystore文件**：`app/release-key.keystore`（自动生成）
- ✅ **签名配置**：在 `app/build.gradle` 中已配置
- ✅ **构建类型**：release版本自动使用签名
- ✅ **AndroidManifest**：已添加必要的 `android:exported` 属性

### 签名信息
- **密钥别名**：release
- **Store密码**：android
- **Key密码**：android
- **有效期**：10000天

⚠️ **注意**：生产环境中请使用更安全的密码并妥善保管keystore文件。

## 脚本特性

### 安全特性
- ✅ **自动备份和恢复**：确保原始文件安全
- ✅ **错误处理**：遇到错误时跳过当前用户，继续处理下一个
- ✅ **签名验证**：自动生成签名APK，可直接安装使用
- ✅ **完整性检查**：验证所有必需文件存在

### 输出信息
- 🔍 **详细日志**：显示每个步骤的详细信息
- 📊 **统计报告**：最后显示成功和失败的统计信息
- 🎨 **彩色输出**：使用不同颜色区分信息类型（仅Shell版本）

### 生成文件
- 📱 **签名APK文件**：在 `userInfos` 目录下生成 `[文件夹名].apk` 文件
- 💾 **备份文件**：在 `backup_original` 目录下保存原始文件备份

## 执行流程

对于每个用户文件夹，脚本执行以下步骤：

1. **文件替换**：将用户文件复制到项目对应位置
2. **清理缓存**：执行 `./gradlew clean` 清理构建缓存
3. **构建APK**：执行 `./gradlew assembleRelease` 构建签名release版本
4. **查找APK**：在 `app/build/outputs/apk/release/` 目录查找生成的APK
5. **重命名复制**：将APK重命名为 `[文件夹名].apk` 并复制到 `userInfos` 目录

## 故障排除

### 常见问题

1. **权限问题**
   ```bash
   chmod +x gradlew build_multiple_apks.sh
   ```

2. **APK无法安装（签名问题）**
   - ✅ 已解决：项目已配置完整的签名设置
   - 生成的APK文件名为 `com.tencent.yolov8ncnn-release.apk`（无 `-unsigned` 后缀）

3. **Android 12+ 兼容性问题**
   - ✅ 已解决：AndroidManifest.xml中已添加必要的 `android:exported` 属性

4. **文件不存在警告**
   - 检查用户文件夹中是否包含所有4个必需文件
   - 确认文件名拼写正确

5. **构建失败**
   - 检查Android SDK环境配置
   - 确认项目依赖是否完整
   - 查看gradle构建日志

### 签名验证

要验证APK是否正确签名，可以使用以下命令：

```bash
# 检查APK基本信息
file userInfos/38.apk

# 使用Android工具验证（如果有SDK）
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging userInfos/38.apk | head -5
```

### 手动清理

如果脚本异常中断，可以手动恢复：

```bash
# 恢复原始文件
cp backup_original/* app/src/main/res/values/
cp backup_original/* app/src/main/assets/res/
cp backup_original/* app/src/main/res/mipmap/

# 清理构建缓存
./gradlew clean
```

## 注意事项

1. **签名安全**：生产环境中请更换为安全的keystore密码
2. **备份重要性**：脚本会自动备份，但建议运行前手动备份重要文件
3. **磁盘空间**：确保有足够空间存储多个APK文件（每个约75-80MB）
4. **构建时间**：每个APK的构建可能需要几分钟，请耐心等待
5. **网络连接**：首次构建可能需要下载依赖包

## 示例输出

```
🚀 开始批量构建APK...
[INFO] 正在备份原始文件...
[INFO] 找到 8 个用户文件夹: [38, 39, 40, 42, 43, 44, 45, 46]

==================================================
[INFO] 正在处理用户文件夹: 38
==================================================
[INFO] 已替换: userInfos/38/config.xml -> app/src/main/res/values/config.xml
[INFO] 正在清理构建缓存...
[SUCCESS] 构建缓存清理完成
[INFO] 正在构建release APK...
[SUCCESS] APK构建完成
[SUCCESS] APK已复制到: userInfos/38.apk
[SUCCESS] ✅ 用户 38 处理完成

============================================================
[INFO] 📊 批量构建完成!
[SUCCESS] ✅ 成功构建: 8 个APK
[ERROR] ❌ 失败数量: 0 个
============================================================
```

## 更新日志

### v1.1 (当前版本)
- ✅ 添加完整的APK签名配置
- ✅ 修复Android 12+兼容性问题
- ✅ 自动生成keystore文件
- ✅ 添加ProGuard混淆配置
- ✅ 优化构建流程和错误处理

### v1.0
- ✅ 基本的批量构建功能
- ✅ 文件替换和备份恢复
- ✅ Python和Shell两个版本 