package com.pioneer.appbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * App构建器主类
 * 用于配置应用参数并生成签名APK
 */
public class AppBuilder {

    private static final String CONFIG_FILE = "app-config.json";
    private static final String RESOURCES_DIR = "resources";
    private static final String ICONS_DIR = RESOURCES_DIR + "/icons";
    private static final String SPLASH_DIR = RESOURCES_DIR + "/splash";
    private static final String SOUNDS_DIR = RESOURCES_DIR + "/sounds";
    private static final String KEYSTORE_DIR = RESOURCES_DIR + "/keystore";
    private static final String OUTPUT_DIR = "output";
    private static final String PROJECT_DIR = "project";
    
    private AppConfig config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // 辅助工具类
    private GradleFileGenerator gradleGenerator;
    private AppResourceManager resourceManager;
    private ApkSigner apkSigner;
    
    public static void main(String[] args) {
        AppBuilder builder = new AppBuilder();
        builder.start();
    }
    
    private void start() {
        printBanner();
        
        // 创建默认目录结构
        createDefaultDirectories();
        
        // 加载或创建配置
        loadOrCreateConfig();
        
        // 初始化辅助工具类
        gradleGenerator = new GradleFileGenerator(config);
        resourceManager = new AppResourceManager(config);
        apkSigner = new ApkSigner(config);
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            printMenu();
            
            int choice = getIntInput(scanner, "请选择操作: ", 0, 7);
            
            switch (choice) {
                case 1:
                    editAppBasicInfo(scanner);
                    break;
                case 2:
                    editConfigXml(scanner);
                    break;
                case 3:
                    configureAppIcon(scanner);
                    break;
                case 4:
                    configureKeystore(scanner);
                    break;
                case 5:
                    buildApk(true);  // 构建带签名的APK
                    break;
                case 6:
                    buildApk(false); // 构建不签名的APK
                    break;
                case 7:
                    saveConfig();
                    System.out.println("配置已保存。");
                    break;
                case 0:
                    saveConfig();
                    running = false;
                    System.out.println("\n感谢使用 Pioneer X App 构建器，再见！");
                    break;
            }
        }
    }
    
    /**
     * 打印启动画面
     */
    private void printBanner() {
        System.out.println("\n" +
                "   ___  _                             __  __    ___                ___      _ __    __         \n" +
                "  / _ \\(_)___  ___  ___ ___ ____    / / / /__ / _ | ___  ___     / _ )__ _(_) /___/ /__ _____ \n" +
                " / ___/ / _ \\/ _ \\/ -_) -_) __/    / /_/ / -_) __ |/ _ \\/ _ \\   / _  / // / / / __/ '_  / -_)\n" +
                "/_/  /_/_//_/_//_/\\__/\\__/_/      \\____/\\__/_/ |_/ .__/ .__/  /____/\\_,_/_/_/\\__/_/\\_\\\\__/ \n" +
                "                                                 /_/  /_/                                      \n");
        System.out.println("============================================= v1.1.0 ==========================================");
        System.out.println("                            Android 应用生成与签名工具                                        ");
        System.out.println("==========================================================================================\n");
    }
    
    /**
     * 创建默认的目录结构
     */
    private void createDefaultDirectories() {
        try {
            // 创建资源目录
            createDirectoryIfNotExists(RESOURCES_DIR);
            createDirectoryIfNotExists(ICONS_DIR);
            createDirectoryIfNotExists(SPLASH_DIR);
            createDirectoryIfNotExists(SOUNDS_DIR);
            createDirectoryIfNotExists(KEYSTORE_DIR);
            createDirectoryIfNotExists(OUTPUT_DIR);
            createDirectoryIfNotExists(PROJECT_DIR);
            
            // 创建README文件，说明每个目录的用途
            createReadmeFiles();
        } catch (IOException e) {
            System.err.println("创建目录结构时出错: " + e.getMessage());
        }
    }
    
    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(String dir) throws IOException {
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("创建目录: " + directory.getAbsolutePath());
        }
    }
    
    /**
     * 创建README文件，说明每个目录的用途
     */
    private void createReadmeFiles() throws IOException {
        // 主README
        String mainReadme = "# App Builder 资源目录\n\n" +
                "这个目录包含了构建自定义APP所需的资源文件。\n\n" +
                "## 目录结构\n\n" +
                "- `icons/`: 存放应用图标，将您的自定义图标放在此处\n" +
                "- `splash/`: 存放启动页图片\n" +
                "- `sounds/`: 存放声音资源\n" +
                "- `keystore/`: 存放应用签名密钥\n" +
                "- `output/`: 构建好的APK将保存在此目录\n" +
                "- `project/`: 存放Android项目源文件\n\n" +
                "修改这些目录中的资源，然后使用App Builder生成您的自定义应用。";
        FileUtils.writeStringToFile(new File(RESOURCES_DIR + "/README.md"), mainReadme, StandardCharsets.UTF_8);
        
        // 图标README
        String iconsReadme = "# 应用图标\n\n" +
                "在此目录中放置您的应用图标文件。\n\n" +
                "## 要求\n\n" +
                "- 推荐使用512x512像素的PNG图片\n" +
                "- 文件名建议为 `app_icon.png`\n";
        FileUtils.writeStringToFile(new File(ICONS_DIR + "/README.md"), iconsReadme, StandardCharsets.UTF_8);
        
        // 启动页README
        String splashReadme = "# 启动页图片\n\n" +
                "在此目录中放置您的应用启动页图片。\n\n" +
                "## 要求\n\n" +
                "- 推荐使用1080x1920像素的JPEG或PNG图片\n" +
                "- 文件名建议为 `splash.jpg` 或 `splash.png`\n";
        FileUtils.writeStringToFile(new File(SPLASH_DIR + "/README.md"), splashReadme, StandardCharsets.UTF_8);
        
        // 声音README
        String soundsReadme = "# 声音资源\n\n" +
                "在此目录中放置您的应用声音资源文件。\n\n" +
                "## 要求\n\n" +
                "- 支持MP3或WAV格式\n" +
                "- 文件名建议为 `detection_sound.mp3`\n";
        FileUtils.writeStringToFile(new File(SOUNDS_DIR + "/README.md"), soundsReadme, StandardCharsets.UTF_8);
        
        // 密钥README
        String keystoreReadme = "# 签名密钥\n\n" +
                "在此目录中放置您的应用签名密钥文件。\n\n" +
                "## 要求\n\n" +
                "- 支持JKS或PKCS12格式的密钥库文件\n" +
                "- 文件名建议为 `app.keystore`\n" +
                "- 如果没有密钥，可以使用App Builder中的功能创建新密钥\n";
        FileUtils.writeStringToFile(new File(KEYSTORE_DIR + "/README.md"), keystoreReadme, StandardCharsets.UTF_8);
        
        // 输出README
        String outputReadme = "# 输出目录\n\n" +
                "构建好的APK将保存在此目录。\n";
        FileUtils.writeStringToFile(new File(OUTPUT_DIR + "/README.md"), outputReadme, StandardCharsets.UTF_8);
        
        // 项目README
        String projectReadme = "# 项目源文件\n\n" +
                "在此目录中放置Android项目源文件。\n\n" +
                "## 说明\n\n" +
                "- 请确保此目录包含一个完整的Android项目\n" +
                "- 项目应包含app目录\n" +
                "- 建议从示例项目复制并修改\n";
        FileUtils.writeStringToFile(new File(PROJECT_DIR + "/README.md"), projectReadme, StandardCharsets.UTF_8);
    }
    
    private void printMenu() {
        System.out.println("\n===== 主菜单 =====");
        System.out.println("1. 编辑应用基本信息（名称、包名、版本）");
        System.out.println("2. 编辑config.xml配置");
        System.out.println("3. 配置应用图标");
        System.out.println("4. 配置签名信息");
        System.out.println("5. 构建签名APK");
        System.out.println("6. 构建不签名APK");
        System.out.println("7. 保存当前配置");
        System.out.println("0. 退出");
        System.out.println("\n提示: 程序会自动从父级目录查找Android项目");
    }
    
    private void loadOrCreateConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                String json = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
                config = gson.fromJson(json, AppConfig.class);
                System.out.println("已加载配置文件。");
            } catch (IOException e) {
                System.err.println("无法读取配置文件: " + e.getMessage());
                config = new AppConfig();
                setDefaultPaths();
            }
        } else {
            config = new AppConfig();
            setDefaultPaths();
            System.out.println("创建新配置文件。");
        }
    }
    
    /**
     * 设置默认路径
     */
    private void setDefaultPaths() {
        // 设置默认的资源路径
        File iconDir = new File(ICONS_DIR);
        File[] iconFiles = iconDir.listFiles((dir, name) -> name.endsWith(".png") && !name.equals("README.md"));
        if (iconFiles != null && iconFiles.length > 0) {
            config.iconPath = iconFiles[0].getAbsolutePath();
        } else {
            config.iconPath = ICONS_DIR + "/app_icon.png";
        }
        
        File splashDir = new File(SPLASH_DIR);
        File[] splashFiles = splashDir.listFiles((dir, name) -> (name.endsWith(".jpg") || name.endsWith(".png")) && !name.equals("README.md"));
        if (splashFiles != null && splashFiles.length > 0) {
            config.splashConfig.imagePath = splashFiles[0].getAbsolutePath();
        } else {
            config.splashConfig.imagePath = SPLASH_DIR + "/splash.jpg";
        }
        
        File keystoreDir = new File(KEYSTORE_DIR);
        File[] keystoreFiles = keystoreDir.listFiles((dir, name) -> name.endsWith(".keystore") && !name.equals("README.md"));
        if (keystoreFiles != null && keystoreFiles.length > 0) {
            config.keystoreConfig.keystorePath = keystoreFiles[0].getAbsolutePath();
        } else {
            config.keystoreConfig.keystorePath = KEYSTORE_DIR + "/app.keystore";
        }
    }
    
    private void saveConfig() {
        try {
            String json = gson.toJson(config);
            FileUtils.writeStringToFile(new File(CONFIG_FILE), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("保存配置时出错: " + e.getMessage());
        }
    }
    
    private void editAppBasicInfo(Scanner scanner) {
        System.out.println("\n===== 编辑应用基本信息 =====");
        
        config.appName = getStringInput(scanner, "应用名称 [" + config.appName + "]: ", config.appName);
        config.packageName = getStringInput(scanner, "包名 [" + config.packageName + "]: ", config.packageName);
        config.versionName = getStringInput(scanner, "版本名称 [" + config.versionName + "]: ", config.versionName);
        config.versionCode = getIntInput(scanner, "版本号 [" + config.versionCode + "]: ", 1, 999999, config.versionCode);
        
        System.out.println("应用基本信息已更新。");
    }
    
    private void editConfigXml(Scanner scanner) {
        System.out.println("\n===== 编辑config.xml配置 =====");
        
        // 启动页配置
        System.out.println("启动页配置:");
        
        // 显示可用的启动页图片
        listAvailableResources(SPLASH_DIR, "图片", ".jpg", ".jpeg", ".png");
        
        config.splashConfig.imagePath = getStringInput(scanner, 
                "启动页图片路径 [" + config.splashConfig.imagePath + "]: ", 
                config.splashConfig.imagePath);
        config.splashConfig.delayTime = getIntInput(scanner, 
                "启动页延迟时间(毫秒) [" + config.splashConfig.delayTime + "]: ", 
                500, 10000, config.splashConfig.delayTime);
        
        // 声音配置
        System.out.println("\n声音配置:");
        
        // 显示可用的声音资源
        listAvailableResources(SOUNDS_DIR, "声音", ".mp3", ".wav");
        
        config.soundConfig.detectionSoundResource = getStringInput(scanner, 
                "检测声音资源 [" + config.soundConfig.detectionSoundResource + "]: ", 
                config.soundConfig.detectionSoundResource);
        
        // App说明页面配置
        System.out.println("\nApp说明页面配置:");
        System.out.println("当前应用说明文本:");
        System.out.println(config.appInfoConfig.infoText);
        System.out.println("输入新的应用说明文本 (输入 '#end' 结束):");
        StringBuilder sb = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equals("#end")) {
            sb.append(line).append("\n");
        }
        if (sb.length() > 0) {
            config.appInfoConfig.infoText = sb.toString();
        }
        
        // 主界面配置
        System.out.println("\n主界面配置:");
        config.mainConfig.defaultTargetLabel = getIntInput(scanner, 
                "默认目标标签 [" + config.mainConfig.defaultTargetLabel + "]: ", 
                0, 100, config.mainConfig.defaultTargetLabel);
        
        System.out.println("config.xml配置已更新。");
    }
    
    /**
     * 列出目录中可用的资源
     * @param directory 目录路径
     * @param resourceType 资源类型描述
     * @param extensions 文件扩展名
     */
    private void listAvailableResources(String directory, String resourceType, String... extensions) {
        System.out.println("可用的" + resourceType + "资源:");
        
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> {
                if (name.equals("README.md")) return false;
                for (String ext : extensions) {
                    if (name.toLowerCase().endsWith(ext)) return true;
                }
                return false;
            });
            
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    System.out.println("  " + (i + 1) + ". " + files[i].getAbsolutePath());
                }
            } else {
                System.out.println("  (无可用资源，请将" + resourceType + "文件放入 " + directory + " 目录)");
            }
        }
    }
    
    private void configureAppIcon(Scanner scanner) {
        System.out.println("\n===== 配置应用图标 =====");
        
        // 显示可用的图标资源
        listAvailableResources(ICONS_DIR, "图标", ".png", ".jpg", ".jpeg");
        
        config.iconPath = getStringInput(scanner, 
                "应用图标路径 (建议使用512x512像素的PNG图片) [" + config.iconPath + "]: ", 
                config.iconPath);
        
        File iconFile = new File(config.iconPath);
        if (iconFile.exists() && iconFile.isFile()) {
            System.out.println("图标文件有效。将在构建时应用。");
        } else {
            System.out.println("警告: 图标文件不存在或不是有效的文件。");
            System.out.println("请将图标文件放入 " + ICONS_DIR + " 目录，或提供完整的文件路径。");
        }
    }
    
    private void configureKeystore(Scanner scanner) {
        System.out.println("\n===== 配置签名信息 =====");
        
        // 显示可用的密钥库
        listAvailableResources(KEYSTORE_DIR, "密钥库", ".keystore", ".jks", ".p12");
        
        config.keystoreConfig.keystorePath = getStringInput(scanner, 
                "Keystore文件路径 [" + config.keystoreConfig.keystorePath + "]: ", 
                config.keystoreConfig.keystorePath);
        
        config.keystoreConfig.keystorePassword = getStringInput(scanner, 
                "Keystore密码 [" + (config.keystoreConfig.keystorePassword.isEmpty() ? "" : "******") + "]: ", 
                config.keystoreConfig.keystorePassword);
        
        config.keystoreConfig.keyAlias = getStringInput(scanner, 
                "Key别名 [" + config.keystoreConfig.keyAlias + "]: ", 
                config.keystoreConfig.keyAlias);
        
        config.keystoreConfig.keyPassword = getStringInput(scanner, 
                "Key密码 [" + (config.keystoreConfig.keyPassword.isEmpty() ? "" : "******") + "]: ", 
                config.keystoreConfig.keyPassword);
        
        File keystoreFile = new File(config.keystoreConfig.keystorePath);
        if (keystoreFile.exists() && keystoreFile.isFile()) {
            System.out.println("Keystore文件有效。");
        } else {
            System.out.println("警告: Keystore文件不存在，是否要创建新的Keystore? (y/n)");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                createNewKeystore(scanner);
            }
        }
    }
    
    private void createNewKeystore(Scanner scanner) {
        System.out.println("\n===== 创建新的Keystore =====");
        String name = getStringInput(scanner, "请输入证书拥有者名称 (CN): ", "");
        String org = getStringInput(scanner, "请输入组织名称 (O): ", "");
        String unit = getStringInput(scanner, "请输入组织单位 (OU): ", "");
        String locality = getStringInput(scanner, "请输入所在地 (L): ", "");
        String state = getStringInput(scanner, "请输入州/省 (ST): ", "");
        String country = getStringInput(scanner, "请输入国家代码 (C): ", "CN");
        
        try {
            // 确保密钥库目录存在
            createDirectoryIfNotExists(KEYSTORE_DIR);
            
            // 如果没有指定密钥库路径，设置为默认路径
            if (config.keystoreConfig.keystorePath.isEmpty()) {
                config.keystoreConfig.keystorePath = KEYSTORE_DIR + "/app.keystore";
            }
            
            String keystore = config.keystoreConfig.keystorePath;
            String storepass = config.keystoreConfig.keystorePassword;
            String keypass = config.keystoreConfig.keyPassword;
            String alias = config.keystoreConfig.keyAlias;
            
            String command = String.format("keytool -genkeypair -v -keystore %s -keyalg RSA -keysize 2048 -validity 10000 " +
                    "-alias %s -storepass %s -keypass %s " +
                    "-dname \"CN=%s, OU=%s, O=%s, L=%s, ST=%s, C=%s\"",
                    keystore, alias, storepass, keypass, name, unit, org, locality, state, country);
            
            Process process = Runtime.getRuntime().exec(command);
            logProcessOutput(process);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("Keystore创建成功！");
            } else {
                System.err.println("Keystore创建失败，错误码: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("创建Keystore时出错: " + e.getMessage());
        }
    }
    
    /**
     * 构建APK
     * @param withSigning 是否签名
     */
    private void buildApk(boolean withSigning) {
        System.out.println("\n===== 构建" + (withSigning ? "签名" : "不签名") + "APK =====");
        
        // 检查基本配置是否完整
        if (!isConfigValid(withSigning)) {
            System.err.println("❌ 配置不完整，无法构建APK。");
            System.err.println("请确保以下配置已完成：");
            System.err.println("- 应用名称和包名");
            System.err.println("- 版本信息");
            if (withSigning) {
                System.err.println("- 签名信息");
            }
            return;
        }
        
        try {
            // 检查项目源文件
            File projectSourceDir = findProjectSourceDirectory();
            if (projectSourceDir == null) {
                System.err.println("错误: 找不到有效的项目源文件。");
                System.err.println("请确保以下位置之一存在有效的Android项目:");
                System.err.println("1. 父级目录 (包含完整项目)");
                System.err.println("2. 当前目录 (包含完整项目)");
                System.err.println("3. 父级目录的app子目录 (仅app模块)");
                System.err.println("4. 当前目录的app子目录 (仅app模块)");
                return;
            }
            
            System.out.println("使用项目源目录: " + projectSourceDir.getAbsolutePath());
            
            // 1. 准备构建目录
            System.out.println("步骤1: 准备构建目录...");
            File buildDir = new File("build");
            if (!buildDir.exists()) {
                buildDir.mkdir();
            }
            
            // 2. 创建临时工作目录
            System.out.println("步骤2: 创建临时工作目录...");
            File tempDir = new File("build/temp");
            if (tempDir.exists()) {
                FileUtils.deleteDirectory(tempDir);
            }
            tempDir.mkdir();
            
            // 3. 准备完整项目结构
            System.out.println("步骤3: 准备项目结构...");
            prepareProjectStructure(projectSourceDir, tempDir);
            
            // 确定app目录位置
            File targetAppDir = new File(tempDir, "app");
            if (!targetAppDir.exists() && isValidAppModuleDir(projectSourceDir)) {
                System.out.println("从app模块创建完整项目结构...");
                // 如果源目录就是app模块，并且在复制后没有创建app目录
                // 则需要将tempDir下的内容移动到app目录
                targetAppDir.mkdir();
                File[] files = tempDir.listFiles(file -> !file.getName().equals("app"));
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            FileUtils.copyDirectoryToDirectory(file, targetAppDir);
                        } else {
                            FileUtils.copyFileToDirectory(file, targetAppDir);
                        }
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
            
            // 4. 应用配置
            System.out.println("步骤4: 应用配置...");
            applyConfigurations(targetAppDir);
            
            // 添加：验证和修复应用结构
            System.out.println("步骤4.1: 验证和修复应用结构...");
            validateAndFixAppStructure(targetAppDir);
            
            // 4.5 禁用可能导致内存问题的lint检查
            System.out.println("步骤4.5: 禁用Lint检查，避免内存问题...");
            disableLintChecks(tempDir);
            
            // 5. 执行Gradle构建
            System.out.println("步骤5: 执行构建...");
            
            // 检查是否有全局的gradle命令
            boolean useGlobalGradle = isGradleAvailable();
            boolean buildSuccess = false;
            
            if (useGlobalGradle) {
                // 使用全局gradle命令
                System.out.println("使用全局Gradle命令构建...");
                buildSuccess = buildWithGlobalGradle(tempDir);
            } else {
                // 尝试使用项目自带的gradlew
                System.out.println("使用项目自带的Gradlew构建...");
                buildSuccess = buildWithGradlew(tempDir);
            }
            
            if (buildSuccess) {
                // 6. 找到生成的APK
                System.out.println("步骤6: 处理构建好的APK...");
                File[] apkFiles = findUnsignedApk(targetAppDir);
                
                if (apkFiles != null && apkFiles.length > 0) {
                    File unsignedApk = apkFiles[0];
                    File outputApk;
                    
                    System.out.println("找到APK: " + unsignedApk.getAbsolutePath());
                    
                    // 创建输出目录
                    File outputDir = new File(OUTPUT_DIR);
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                        System.out.println("创建输出目录: " + outputDir.getAbsolutePath());
                    }
                    
                    if (withSigning) {
                        // 对APK进行签名
                        System.out.println("步骤7: 对APK进行签名...");
                        outputApk = new File(OUTPUT_DIR + "/" + config.appName + "-" + config.versionName + "-signed.apk");
                        boolean signSuccess = apkSigner.signApk(unsignedApk, outputApk);
                        
                        if (signSuccess) {
                            System.out.println("\n✅ APK构建并签名成功！");
                            System.out.println("签名APK文件位置: " + outputApk.getAbsolutePath());
                        } else {
                            System.err.println("\n❌ APK签名失败。");
                            System.err.println("可能原因: 无效的签名配置或权限问题");
                            
                            // 尝试直接复制未签名的APK作为备选方案
                            try {
                                File fallbackApk = new File(OUTPUT_DIR + "/" + config.appName + "-" + config.versionName + "-unsigned.apk");
                                FileUtils.copyFile(unsignedApk, fallbackApk);
                                System.out.println("已复制未签名APK到: " + fallbackApk.getAbsolutePath());
                            } catch (IOException e) {
                                System.err.println("复制未签名APK失败: " + e.getMessage());
                            }
                        }
                    } else {
                        // 直接复制未签名的APK
                        System.out.println("步骤7: 复制未签名APK...");
                        outputApk = new File(OUTPUT_DIR + "/" + config.appName + "-" + config.versionName + "-unsigned.apk");
                        FileUtils.copyFile(unsignedApk, outputApk);
                        System.out.println("\n✅ APK构建成功！");
                        System.out.println("未签名APK文件位置: " + outputApk.getAbsolutePath());
                    }
                    
                    // 如果找到多个APK，显示其他APK的信息
                    if (apkFiles.length > 1) {
                        System.out.println("\n另外还找到了 " + (apkFiles.length - 1) + " 个APK文件：");
                        for (int i = 1; i < apkFiles.length; i++) {
                            System.out.println((i) + ". " + apkFiles[i].getAbsolutePath());
                        }
                    }
                } else {
                    System.err.println("\n❌ 无法找到生成的APK文件。");
                    System.err.println("请检查构建日志以获取详细信息。");
                    
                    // 尝试在整个项目目录中搜索APK
                    List<File> allApks = new ArrayList<>();
                    findApkFilesRecursively(new File("build/temp"), allApks);
                    
                    if (!allApks.isEmpty()) {
                        System.out.println("\n在整个项目中找到了 " + allApks.size() + " 个APK文件：");
                        for (int i = 0; i < allApks.size(); i++) {
                            File apk = allApks.get(i);
                            System.out.println((i + 1) + ". " + apk.getAbsolutePath());
                            
                            // 复制第一个找到的APK到输出目录
                            if (i == 0) {
                                try {
                                    File outputDir = new File(OUTPUT_DIR);
                                    if (!outputDir.exists()) {
                                        outputDir.mkdirs();
                                    }
                                    
                                    File outputApk = new File(OUTPUT_DIR + "/" + config.appName + "-" + config.versionName + 
                                                         (withSigning ? "-signed.apk" : "-unsigned.apk"));
                                    FileUtils.copyFile(apk, outputApk);
                                    System.out.println("已复制APK到: " + outputApk.getAbsolutePath());
                                } catch (IOException e) {
                                    System.err.println("复制APK失败: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            } else {
                System.err.println("\n❌ APK构建失败。");
                System.err.println("常见原因:");
                System.err.println("1. Java版本不兼容 (Android Gradle插件要求Java 11+)");
                System.err.println("2. Gradle版本过旧或不兼容");
                System.err.println("3. Android SDK路径配置错误");
                System.err.println("4. 缺少必要的构建依赖");
                System.err.println("请检查日志获取详细错误信息。");
            }
        } catch (Exception e) {
            System.err.println("\n❌ 构建APK时出错: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getName());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证和修复应用结构
     * @param appDir 应用目录
     * @throws IOException IO异常
     */
    private void validateAndFixAppStructure(File appDir) throws IOException {
        // 1. 确保SplashActivity类存在
        resourceManager.ensureActivityExists(appDir, config.packageName);
        
        // 2. 修复AndroidManifest.xml中的活动声明
        resourceManager.fixManifestActivityDeclaration(appDir, config.packageName);
        
        // 3. 确保资源文件存在
        resourceManager.ensureRequiredResources(appDir);
        
        // 4. 检查R类引用
        String oldPackageName = resourceManager.detectOldPackageName(appDir);
        if (oldPackageName != null && !oldPackageName.equals(config.packageName)) {
            System.out.println("检测到包名变更: " + oldPackageName + " -> " + config.packageName);
            resourceManager.updateRImports(appDir, oldPackageName, config.packageName);
        }
        
        System.out.println("应用结构验证和修复完成 ✓");
    }
    
    /**
     * 检查配置是否有效
     * @param checkSigning 是否检查签名配置
     * @return 配置是否有效
     */
    private boolean isConfigValid(boolean checkSigning) {
        // 检查基本信息
        if (StringUtils.isBlank(config.appName) || StringUtils.isBlank(config.packageName) || 
                StringUtils.isBlank(config.versionName) || config.versionCode <= 0) {
            return false;
        }
        
        // 如果需要签名，检查签名配置
        if (checkSigning) {
            // 检查密钥库文件是否存在
            File keystoreFile = new File(config.keystoreConfig.keystorePath);
            if (!keystoreFile.exists() || !keystoreFile.isFile()) {
                return false;
            }
            
            // 检查签名密码等信息
            if (StringUtils.isBlank(config.keystoreConfig.keystorePassword) || 
                    StringUtils.isBlank(config.keystoreConfig.keyAlias) || 
                    StringUtils.isBlank(config.keystoreConfig.keyPassword)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查系统中是否安装了Gradle
     * @return 是否可用
     */
    private boolean isGradleAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("gradle", "--version");
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 使用全局gradle命令构建APK
     * @param appDir 应用目录
     * @return 是否构建成功
     */
    private boolean buildWithGlobalGradle(File appDir) {
        try {
            System.out.println("使用全局Gradle命令构建...");
            
            // 使用更大的JVM内存参数，避免内存不足
            ProcessBuilder processBuilder = new ProcessBuilder(
                "gradle", 
                "-Dorg.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError",
                "clean", 
                "assembleRelease", 
                "--no-daemon", 
                "--stacktrace");
            processBuilder.directory(appDir);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            logProcessOutput(process);
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("使用全局gradle构建失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用项目自带的gradlew构建APK
     * @param appDir 应用目录
     * @return 是否构建成功
     */
    private boolean buildWithGradlew(File appDir) {
        try {
            System.out.println("尝试使用项目自带的gradlew构建...");
            
            // 检查是否存在gradlew文件
            File gradlew = new File("build/temp/gradlew");
            if (!gradlew.exists()) {
                // 尝试从原始项目目录查找gradlew
                File projectGradlew = findGradlewFile();
                if (projectGradlew != null && projectGradlew.exists()) {
                    // 复制gradlew到临时目录
                    FileUtils.copyFile(projectGradlew, gradlew);
                    // 设置执行权限
                    gradlew.setExecutable(true);
                    
                    // 复制gradlew.bat (如果存在)
                    File projectGradlewBat = new File(projectGradlew.getParentFile(), "gradlew.bat");
                    if (projectGradlewBat.exists()) {
                        FileUtils.copyFile(projectGradlewBat, new File("build/temp/gradlew.bat"));
                    }
                    
                    // 复制gradle目录 (如果存在)
                    File projectGradleDir = new File(projectGradlew.getParentFile(), "gradle");
                    if (projectGradleDir.exists() && projectGradleDir.isDirectory()) {
                        FileUtils.copyDirectory(projectGradleDir, new File("build/temp/gradle"));
                    }
                } else {
                    // 创建一个简单的gradle包装器脚本
                    createSimpleGradleWrapper(gradlew);
                }
            }
            
            // 构建命令
            String gradlewCmd = "./gradlew";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                gradlewCmd = "gradlew.bat";
            }
            
            // 创建或更新gradle.properties文件，增加内存配置
            updateGradleProperties(new File("build/temp/gradle.properties"));
            
            // 使用更大的JVM内存，禁用检查，避免内存不足
            ProcessBuilder processBuilder = new ProcessBuilder(
                gradlewCmd, 
                "clean", 
                "assembleRelease", 
                "-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m",
                "-Pandroid.lint.abortOnError=false",
                "--no-daemon",
                "--stacktrace");
            processBuilder.directory(new File("build/temp"));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            logProcessOutput(process);
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("使用gradlew构建失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新gradle.properties文件，添加内存配置
     * @param propertiesFile 属性文件
     * @throws IOException IO异常
     */
    private void updateGradleProperties(File propertiesFile) throws IOException {
        StringBuilder content = new StringBuilder();
        
        if (propertiesFile.exists()) {
            String existingContent = FileUtils.readFileToString(propertiesFile, StandardCharsets.UTF_8);
            content.append(existingContent);
            if (!existingContent.endsWith("\n")) {
                content.append("\n");
            }
        }
        
        // 添加内存配置
        if (!content.toString().contains("org.gradle.jvmargs")) {
            content.append("org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError\n");
        }
        
        // 禁用Lint检查
        if (!content.toString().contains("android.lint.abortOnError")) {
            content.append("android.lint.abortOnError=false\n");
        }
        
        // 启用并行构建
        if (!content.toString().contains("org.gradle.parallel")) {
            content.append("org.gradle.parallel=true\n");
        }
        
        // 启用构建缓存
        if (!content.toString().contains("org.gradle.caching")) {
            content.append("org.gradle.caching=true\n");
        }
        
        // 使用AndroidX
        if (!content.toString().contains("android.useAndroidX")) {
            content.append("android.useAndroidX=true\n");
        }
        
        FileUtils.writeStringToFile(propertiesFile, content.toString(), StandardCharsets.UTF_8);
        System.out.println("更新gradle.properties内存配置");
    }
    
    /**
     * 查找项目中的gradlew文件
     * @return gradlew文件
     */
    private File findGradlewFile() {
        // 检查当前目录
        File currentGradlew = new File("gradlew");
        if (currentGradlew.exists()) {
            return currentGradlew;
        }
        
        // 检查父目录
        File parentGradlew = new File("../gradlew");
        if (parentGradlew.exists()) {
            return parentGradlew;
        }
        
        // 检查project目录
        File projectGradlew = new File(PROJECT_DIR + "/gradlew");
        if (projectGradlew.exists()) {
            return projectGradlew;
        }
        
        return null;
    }
    
    /**
     * 创建简单的gradle包装器脚本
     * @param gradlewFile 脚本文件
     * @throws IOException IO异常
     */
    private void createSimpleGradleWrapper(File gradlewFile) throws IOException {
        System.out.println("创建简单的gradle包装器脚本...");
        
        // 为不同操作系统创建不同的脚本
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows批处理脚本
            String batchContent = "@echo off\n" +
                    "gradle %*\n";
            FileUtils.writeStringToFile(gradlewFile, batchContent, StandardCharsets.UTF_8);
        } else {
            // Unix shell脚本
            String shellContent = "#!/bin/sh\n" +
                    "gradle \"$@\"\n";
            FileUtils.writeStringToFile(gradlewFile, shellContent, StandardCharsets.UTF_8);
            gradlewFile.setExecutable(true);
        }
    }
    
    /**
     * 查找项目源目录
     * 优先顺序:
     * 1. 父级目录 (包含settings.gradle的完整项目)
     * 2. 当前目录 (如果包含settings.gradle)
     * 3. 父级目录的app子目录 (如果存在)
     * 4. 当前目录的app子目录 (如果存在)
     * @return 项目源目录
     */
    private File findProjectSourceDirectory() {
        // 查找父级目录是否是完整项目
        File parentDir = new File("..").getAbsoluteFile();
        if (isValidRootProjectDir(parentDir)) {
            System.out.println("找到完整项目目录: " + parentDir.getAbsolutePath());
            return parentDir;
        }
        
        // 查找父级目录的父级目录是否是完整项目
        File grandParentDir = parentDir.getParentFile();
        if (grandParentDir != null && isValidRootProjectDir(grandParentDir)) {
            System.out.println("找到完整项目目录: " + grandParentDir.getAbsolutePath());
            return grandParentDir;
        }
        
        // 检查当前目录是否是完整项目
        File currentDir = new File(".").getAbsoluteFile();
        if (isValidRootProjectDir(currentDir)) {
            System.out.println("找到完整项目目录: " + currentDir.getAbsolutePath());
            return currentDir;
        }
        
        // 检查父级目录的app子目录
        File parentAppDir = new File(parentDir, "app");
        if (isValidAppModuleDir(parentAppDir)) {
            System.out.println("警告: 只找到app模块，没有找到完整项目。将尝试创建最小项目结构。");
            System.out.println("使用父级目录的app模块: " + parentAppDir.getAbsolutePath());
            return parentAppDir;
        }
        
        // 检查当前目录的app子目录
        File currentAppDir = new File("app");
        if (isValidAppModuleDir(currentAppDir)) {
            System.out.println("警告: 只找到app模块，没有找到完整项目。将尝试创建最小项目结构。");
            System.out.println("使用当前目录的app模块: " + currentAppDir.getAbsolutePath());
            return currentAppDir;
        }
        
        return null;
    }
    
    /**
     * 检查目录是否是有效的项目根目录
     * @param dir 目录
     * @return 是否有效
     */
    private boolean isValidRootProjectDir(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        
        // 检查settings.gradle文件是否存在
        File settingsGradle = new File(dir, "settings.gradle");
        File settingsGradleKts = new File(dir, "settings.gradle.kts");
        if (!settingsGradle.exists() && !settingsGradleKts.exists()) {
            return false;
        }
        
        // 检查是否包含app目录
        File appDir = new File(dir, "app");
        return appDir.exists() && appDir.isDirectory() && isValidAppModuleDir(appDir);
    }
    
    /**
     * 检查目录是否是有效的app模块目录
     * @param dir 目录
     * @return 是否有效
     */
    private boolean isValidAppModuleDir(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        
        // 检查src/main目录是否存在
        File srcMainDir = new File(dir, "src/main");
        if (!srcMainDir.exists() || !srcMainDir.isDirectory()) {
            return false;
        }
        
        // 检查build.gradle文件是否存在
        File buildGradle = new File(dir, "build.gradle");
        File buildGradleKts = new File(dir, "build.gradle.kts");
        
        return buildGradle.exists() || buildGradleKts.exists();
    }
    
    /**
     * 准备完整的项目结构
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @throws IOException IO异常
     */
    private void prepareProjectStructure(File sourceDir, File targetDir) throws IOException {
        // 检查源目录是否是完整项目
        boolean isFullProject = isValidRootProjectDir(sourceDir);
        
        // 检测当前Java版本
        String javaVersion = System.getProperty("java.version");
        boolean isJava11OrHigher = isJavaVersionAtLeast11(javaVersion);
        System.out.println("当前Java版本: " + javaVersion);
        
        if (isFullProject) {
            // 复制整个项目
            FileUtils.copyDirectory(sourceDir, targetDir);
        } else {
            // 只找到app模块，需要创建最小的项目结构
            System.out.println("创建最小项目结构...");
            
            // 创建app目录
            File targetAppDir = new File(targetDir, "app");
            
            // 复制app模块
            FileUtils.copyDirectory(sourceDir, targetAppDir);
            
            // 创建settings.gradle
            String settingsContent = "rootProject.name = '" + config.packageName + "'\n" +
                    "include ':app'\n";
            FileUtils.writeStringToFile(new File(targetDir, "settings.gradle"), 
                    settingsContent, StandardCharsets.UTF_8);
            
            // 创建根目录build.gradle，根据Java版本选择合适的Gradle插件版本
            String gradleVersion = isJava11OrHigher ? "7.0.4" : "4.1.3";
            String rootBuildContent = "// 根项目build.gradle\n" +
                    "buildscript {\n" +
                    "    repositories {\n" +
                    "        google()\n" +
                    "        mavenCentral()\n" +
                    "    }\n" +
                    "    dependencies {\n" +
                    "        classpath 'com.android.tools.build:gradle:" + gradleVersion + "'\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "allprojects {\n" +
                    "    repositories {\n" +
                    "        google()\n" +
                    "        mavenCentral()\n" +
                    "    }\n" +
                    "}\n";
            FileUtils.writeStringToFile(new File(targetDir, "build.gradle"), 
                    rootBuildContent, StandardCharsets.UTF_8);
            
            // 创建gradle.properties
            String gradleProperties = "# Gradle properties\n" +
                    "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n" +
                    "android.useAndroidX=true\n" +
                    "android.enableJetifier=true\n";
            FileUtils.writeStringToFile(new File(targetDir, "gradle.properties"), 
                    gradleProperties, StandardCharsets.UTF_8);
            
            // 创建local.properties（如果能获取到Android SDK路径）
            String sdkPath = System.getenv("ANDROID_HOME");
            if (sdkPath != null && !sdkPath.isEmpty()) {
                String localProperties = "sdk.dir=" + sdkPath.replace("\\", "\\\\") + "\n";
                FileUtils.writeStringToFile(new File(targetDir, "local.properties"), 
                        localProperties, StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 检查Java版本是否为11或更高
     * @param versionString Java版本字符串
     * @return 是否为Java 11或更高版本
     */
    private boolean isJavaVersionAtLeast11(String versionString) {
        // 处理形如 "1.8.0_292" 的旧版格式
        if (versionString.startsWith("1.")) {
            return false; // 1.x都低于11
        }
        
        // 处理形如 "11.0.12" 或 "17.0.1" 的新版格式
        try {
            int majorVersion;
            // 提取主版本号
            if (versionString.contains(".")) {
                majorVersion = Integer.parseInt(versionString.substring(0, versionString.indexOf('.')));
            } else {
                majorVersion = Integer.parseInt(versionString);
            }
            return majorVersion >= 11;
        } catch (NumberFormatException e) {
            System.err.println("无法解析Java版本: " + versionString);
            return false; // 解析失败时默认为不兼容
        }
    }
    
    /**
     * 应用所有配置到项目
     * @param appDir 应用目录
     * @throws IOException IO异常
     */
    private void applyConfigurations(File appDir) throws IOException {
        System.out.println("应用配置...");
        
        // 1. 更新build.gradle文件
        gradleGenerator.updateAppBuildGradle(new File(appDir, "build.gradle").getAbsolutePath());
        
        // 2. 更新AndroidManifest.xml
        resourceManager.updateManifest(appDir);
        
        // 3. 更新strings.xml (应用名称)
        resourceManager.updateAppName(appDir);
        
        // 4. 更新config.xml
        resourceManager.updateConfigXml(appDir);
        
        // 5. 更新启动页图片
        resourceManager.updateSplashImage(appDir);
        
        // 6. 更新应用图标
        resourceManager.updateAppIcon(appDir);
        
        // 7. 确保所有必要的资源文件存在
        resourceManager.ensureRequiredResources(appDir);
        
        System.out.println("配置应用完成。");
    }
    
    /**
     * 记录进程输出
     * @param process 要记录的进程
     * @throws IOException IO异常
     */
    private void logProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
    
    // 工具方法
    
    private String getStringInput(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    private int getIntInput(Scanner scanner, String prompt, int min, int max) {
        return getIntInput(scanner, prompt, min, max, min);
    }
    
    private int getIntInput(Scanner scanner, String prompt, int min, int max, int defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                return defaultValue;
            }
            
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println("请输入" + min + "到" + max + "之间的数字。");
                }
            } catch (NumberFormatException e) {
                System.out.println("请输入有效的数字。");
            }
        }
    }
    
    /**
     * 查找生成的未签名APK
     * @param appDir 应用目录
     * @return 未签名APK文件数组
     */
    private File[] findUnsignedApk(File appDir) {
        System.out.println("搜索生成的APK文件...");

        // 可能的APK输出路径列表
        String[] possiblePaths = {
            "app/build/outputs/apk/release",
            "build/outputs/apk/release",
            "outputs/apk/release",
            "app/build/outputs/apk/debug",
            "build/outputs/apk/debug",
            "outputs/apk/debug"
        };

        // 尝试每个可能的路径
        for (String path : possiblePaths) {
            File outputDir = new File(appDir, path);
            System.out.println("检查目录: " + outputDir.getAbsolutePath());

            if (outputDir.exists() && outputDir.isDirectory()) {
                // 查找任何APK文件
                File[] apkFiles = outputDir.listFiles(file -> 
                        file.isFile() && file.getName().endsWith(".apk"));
                
                if (apkFiles != null && apkFiles.length > 0) {
                    System.out.println("在 " + path + " 中找到 " + apkFiles.length + " 个APK文件");
                    return apkFiles;
                }
            }
        }

        // 如果在常规位置找不到，尝试递归搜索build目录
        System.out.println("在常规位置未找到APK，尝试递归搜索...");
        File buildDir = new File(appDir, "build");
        if (buildDir.exists()) {
            List<File> foundApks = new ArrayList<>();
            findApkFilesRecursively(buildDir, foundApks);
            
            if (!foundApks.isEmpty()) {
                System.out.println("递归搜索找到 " + foundApks.size() + " 个APK文件");
                return foundApks.toArray(new File[0]);
            }
        }

        // 再尝试扫描app目录下的build目录
        File appModuleDir = new File(appDir, "app");
        if (appModuleDir.exists()) {
            File appBuildDir = new File(appModuleDir, "build");
            if (appBuildDir.exists()) {
                List<File> foundApks = new ArrayList<>();
                findApkFilesRecursively(appBuildDir, foundApks);
                
                if (!foundApks.isEmpty()) {
                    System.out.println("在app/build中找到 " + foundApks.size() + " 个APK文件");
                    return foundApks.toArray(new File[0]);
                }
            }
        }

        System.out.println("未找到任何APK文件");
        return null;
    }

    /**
     * 递归搜索目录中的APK文件
     * @param dir 搜索目录
     * @param foundApks 找到的APK文件列表
     */
    private void findApkFilesRecursively(File dir, List<File> foundApks) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".apk")) {
                foundApks.add(file);
                System.out.println("找到APK: " + file.getAbsolutePath());
            } else if (file.isDirectory()) {
                findApkFilesRecursively(file, foundApks);
            }
        }
    }
    
    /**
     * 禁用Lint检查以避免内存问题
     * @param projectDir 项目目录
     * @throws IOException IO异常
     */
    private void disableLintChecks(File projectDir) throws IOException {
        // 1. 修改app/build.gradle
        File appBuildGradleFile = new File(projectDir, "app/build.gradle");
        if (appBuildGradleFile.exists()) {
            String content = FileUtils.readFileToString(appBuildGradleFile, StandardCharsets.UTF_8);
            
            // 如果没有lintOptions块，添加一个
            if (!content.contains("lintOptions")) {
                // 查找android闭合块的位置
                int androidBlockStart = content.indexOf("android {");
                if (androidBlockStart != -1) {
                    int insertPos = content.indexOf("}", androidBlockStart);
                    if (insertPos != -1) {
                        String lintOptions = "\n    lintOptions {\n" +
                                             "        checkReleaseBuilds false\n" +
                                             "        abortOnError false\n" +
                                             "        disable 'MissingTranslation', 'ExtraTranslation'\n" +
                                             "    }\n";
                        content = content.substring(0, insertPos) + lintOptions + content.substring(insertPos);
                        FileUtils.writeStringToFile(appBuildGradleFile, content, StandardCharsets.UTF_8);
                        System.out.println("已添加lintOptions到app/build.gradle");
                    }
                }
            }
        }
        
        // 2. 修改gradle.properties
        File gradlePropertiesFile = new File(projectDir, "gradle.properties");
        if (!gradlePropertiesFile.exists()) {
            FileUtils.touch(gradlePropertiesFile);
        }
        
        String properties = FileUtils.readFileToString(gradlePropertiesFile, StandardCharsets.UTF_8);
        if (!properties.contains("android.lint.abortOnError")) {
            properties += "\n# 禁用Lint错误中断构建\nandroid.lint.abortOnError=false\n";
        }
        
        // 增加JVM内存配置
        if (!properties.contains("org.gradle.jvmargs")) {
            properties += "\n# 增加JVM内存配置\norg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError\n";
        }
        
        // 禁用守护进程
        if (!properties.contains("org.gradle.daemon")) {
            properties += "\n# 禁用Gradle守护进程\norg.gradle.daemon=false\n";
        }
        
        FileUtils.writeStringToFile(gradlePropertiesFile, properties, StandardCharsets.UTF_8);
        System.out.println("已更新gradle.properties禁用Lint检查");
    }
} 