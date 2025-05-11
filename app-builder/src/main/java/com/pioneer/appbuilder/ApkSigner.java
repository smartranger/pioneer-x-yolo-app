package com.pioneer.appbuilder;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * APK签名工具
 * 用于处理APK签名相关的操作
 */
public class ApkSigner {
    
    private final AppConfig config;
    
    public ApkSigner(AppConfig config) {
        this.config = config;
    }
    
    /**
     * 对APK文件进行签名
     * @param unsignedApkFile 未签名的APK文件
     * @param outputFile 签名后的输出文件
     * @return 是否签名成功
     * @throws IOException IO异常
     * @throws InterruptedException 进程中断异常
     */
    public boolean signApk(File unsignedApkFile, File outputFile) throws IOException, InterruptedException {
        System.out.println("\n===== APK签名过程 =====");
        System.out.println("源APK: " + unsignedApkFile.getAbsolutePath());
        System.out.println("目标APK: " + outputFile.getAbsolutePath());
        
        if (!isSigningConfigValid()) {
            System.err.println("错误: 签名配置无效，无法进行签名。");
            System.err.println("密钥库路径: " + config.keystoreConfig.keystorePath);
            System.err.println("密钥库是否存在: " + new File(config.keystoreConfig.keystorePath).exists());
            return false;
        }
        
        // 先验证APK是否有效
        if (!validateApk(unsignedApkFile)) {
            System.err.println("错误: 待签名的APK文件无效或损坏。");
            return false;
        }
        
        // 尝试使用zipalign对齐APK（可能提高性能）
        File alignedApk = alignApk(unsignedApkFile);
        
        // 确定要签名的APK文件
        File apkToSign = alignedApk != null ? alignedApk : unsignedApkFile;
        
        // 构建签名命令
        boolean signSuccess;
        
        // 检查是否使用新版的apksigner工具
        if (isApkSignerAvailable()) {
            signSuccess = signWithApkSigner(apkToSign, outputFile);
        } else {
            signSuccess = signWithJarsigner(apkToSign, outputFile);
        }
        
        // 如果使用了zipalign且签名成功，删除中间文件
        if (alignedApk != null && signSuccess && !alignedApk.equals(unsignedApkFile)) {
            alignedApk.delete();
        }
        
        // 验证签名后的APK
        if (signSuccess) {
            signSuccess = validateSignedApk(outputFile);
            
            if (signSuccess) {
                printInstallInstructions(outputFile);
            }
        }
        
        return signSuccess;
    }
    
    /**
     * 使用旧版jarsigner进行签名
     * @param unsignedApkFile 未签名APK
     * @param outputFile 输出文件
     * @return 是否成功
     * @throws IOException IO异常
     * @throws InterruptedException 进程中断异常
     */
    private boolean signWithJarsigner(File unsignedApkFile, File outputFile) throws IOException, InterruptedException {
        System.out.println("使用jarsigner进行APK签名...");
        
        List<String> command = new ArrayList<>();
        command.add("jarsigner");
        command.add("-verbose");
        command.add("-sigalg");
        command.add("SHA1withRSA");
        command.add("-digestalg");
        command.add("SHA1");
        command.add("-keystore");
        command.add(config.keystoreConfig.keystorePath);
        command.add("-storepass");
        command.add(config.keystoreConfig.keystorePassword);
        command.add("-keypass");
        command.add(config.keystoreConfig.keyPassword);
        command.add("-signedjar");
        command.add(outputFile.getAbsolutePath());
        command.add(unsignedApkFile.getAbsolutePath());
        command.add(config.keystoreConfig.keyAlias);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        logProcessOutput(process);
        
        int exitCode = process.waitFor();
        return exitCode == 0;
    }
    
    /**
     * 使用新版apksigner进行签名
     * @param unsignedApkFile 未签名APK
     * @param outputFile 输出文件
     * @return 是否成功
     * @throws IOException IO异常
     * @throws InterruptedException 进程中断异常
     */
    private boolean signWithApkSigner(File unsignedApkFile, File outputFile) throws IOException, InterruptedException {
        System.out.println("使用apksigner进行APK签名...");
        
        // 首先复制未签名的APK到输出位置
        FileUtils.copyFile(unsignedApkFile, outputFile);
        
        List<String> command = new ArrayList<>();
        command.add("apksigner");
        command.add("sign");
        command.add("--ks");
        command.add(config.keystoreConfig.keystorePath);
        command.add("--ks-pass");
        command.add("pass:" + config.keystoreConfig.keystorePassword);
        command.add("--key-pass");
        command.add("pass:" + config.keystoreConfig.keyPassword);
        command.add("--ks-key-alias");
        command.add(config.keystoreConfig.keyAlias);
        command.add(outputFile.getAbsolutePath());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        logProcessOutput(process);
        
        int exitCode = process.waitFor();
        
        // 验证签名
        if (exitCode == 0) {
            return verifyApkSignature(outputFile);
        } else {
            return false;
        }
    }
    
    /**
     * 验证APK签名
     * @param apkFile 已签名的APK文件
     * @return 签名是否有效
     * @throws IOException IO异常
     * @throws InterruptedException 进程中断异常
     */
    private boolean verifyApkSignature(File apkFile) throws IOException, InterruptedException {
        System.out.println("验证APK签名...");
        
        List<String> command = new ArrayList<>();
        command.add("apksigner");
        command.add("verify");
        command.add("--verbose");
        command.add(apkFile.getAbsolutePath());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        logProcessOutput(process);
        
        int exitCode = process.waitFor();
        return exitCode == 0;
    }
    
    /**
     * 检查是否可以使用apksigner工具
     * @return 是否可用
     */
    private boolean isApkSignerAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("apksigner", "--version");
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            process.waitFor();
            
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查签名配置是否有效
     * @return 是否有效
     */
    private boolean isSigningConfigValid() {
        if (config.keystoreConfig == null) {
            return false;
        }
        
        if (config.keystoreConfig.keystorePath == null || config.keystoreConfig.keystorePath.isEmpty()) {
            return false;
        }
        
        File keystoreFile = new File(config.keystoreConfig.keystorePath);
        if (!keystoreFile.exists() || !keystoreFile.isFile()) {
            return false;
        }
        
        if (config.keystoreConfig.keystorePassword == null || config.keystoreConfig.keystorePassword.isEmpty()) {
            return false;
        }
        
        if (config.keystoreConfig.keyAlias == null || config.keystoreConfig.keyAlias.isEmpty()) {
            return false;
        }
        
        if (config.keystoreConfig.keyPassword == null || config.keystoreConfig.keyPassword.isEmpty()) {
            return false;
        }
        
        return true;
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
    
    /**
     * 对APK进行zipalign对齐
     * @param apkFile 原始APK文件
     * @return 对齐后的APK文件，如果对齐失败则返回原始文件
     */
    private File alignApk(File apkFile) {
        try {
            System.out.println("尝试对APK进行zipalign对齐...");
            
            File alignedFile = new File(apkFile.getParentFile(), 
                    apkFile.getName().replace(".apk", "-aligned.apk"));
            
            // 检查zipalign是否可用
            ProcessBuilder checkBuilder = new ProcessBuilder("zipalign", "-v", "-h");
            checkBuilder.redirectErrorStream(true);
            Process checkProcess = checkBuilder.start();
            int exitCode = checkProcess.waitFor();
            
            if (exitCode != 0) {
                System.out.println("zipalign工具不可用，跳过对齐步骤");
                return apkFile;
            }
            
            // 执行zipalign对齐
            ProcessBuilder alignBuilder = new ProcessBuilder(
                    "zipalign", "-v", "-f", "4", apkFile.getAbsolutePath(), alignedFile.getAbsolutePath());
            alignBuilder.redirectErrorStream(true);
            
            Process alignProcess = alignBuilder.start();
            logProcessOutput(alignProcess);
            
            exitCode = alignProcess.waitFor();
            
            if (exitCode == 0 && alignedFile.exists() && alignedFile.length() > 0) {
                System.out.println("APK对齐成功");
                return alignedFile;
            } else {
                System.out.println("APK对齐失败，使用原始APK继续");
                if (alignedFile.exists()) {
                    alignedFile.delete();
                }
                return apkFile;
            }
        } catch (Exception e) {
            System.out.println("执行zipalign时出错: " + e.getMessage());
            return apkFile;
        }
    }
    
    /**
     * 验证APK是否有效
     * @param apkFile APK文件
     * @return 是否有效
     */
    private boolean validateApk(File apkFile) {
        try {
            System.out.println("验证APK完整性...");
            
            // 使用aapt检查APK
            ProcessBuilder aaptBuilder = new ProcessBuilder("aapt", "list", apkFile.getAbsolutePath());
            aaptBuilder.redirectErrorStream(true);
            
            Process aaptProcess = aaptBuilder.start();
            
            // 检查是否有AndroidManifest.xml
            boolean hasManifest = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(aaptProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("AndroidManifest.xml")) {
                        hasManifest = true;
                        break;
                    }
                }
            }
            
            aaptProcess.waitFor();
            
            if (!hasManifest) {
                System.err.println("APK文件中没有找到AndroidManifest.xml");
                return false;
            }
            
            // 使用aapt dump验证更多信息
            ProcessBuilder dumpBuilder = new ProcessBuilder("aapt", "dump", "badging", apkFile.getAbsolutePath());
            dumpBuilder.redirectErrorStream(true);
            
            Process dumpProcess = dumpBuilder.start();
            
            // 提取应用信息
            String packageName = "";
            String versionName = "";
            String sdkVersion = "";
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(dumpProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("package:")) {
                        // 提取包名和版本
                        if (line.contains("name='") && line.contains("'")) {
                            packageName = line.split("name='")[1].split("'")[0];
                        }
                        if (line.contains("versionName='") && line.contains("'")) {
                            versionName = line.split("versionName='")[1].split("'")[0];
                        }
                    } else if (line.startsWith("sdkVersion:")) {
                        // 提取最小SDK版本
                        sdkVersion = line.split("'")[1];
                    }
                }
            }
            
            dumpProcess.waitFor();
            
            System.out.println("APK信息:");
            System.out.println("  包名: " + packageName);
            System.out.println("  版本: " + versionName);
            System.out.println("  最小SDK: " + sdkVersion);
            
            return true;
        } catch (Exception e) {
            System.err.println("验证APK时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证签名后的APK
     * @param apkFile 已签名的APK文件
     * @return 签名是否有效
     */
    private boolean validateSignedApk(File apkFile) {
        try {
            System.out.println("\n验证APK签名...");
            
            // 使用apksigner验证签名
            if (isApkSignerAvailable()) {
                ProcessBuilder verifyBuilder = new ProcessBuilder(
                        "apksigner", "verify", "--verbose", apkFile.getAbsolutePath());
                verifyBuilder.redirectErrorStream(true);
                
                Process verifyProcess = verifyBuilder.start();
                boolean isValid = false;
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(verifyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("Verified using v1 scheme") || 
                            line.contains("Verified using v2 scheme") ||
                            line.contains("Verified using v3 scheme")) {
                            isValid = true;
                        }
                    }
                }
                
                int exitCode = verifyProcess.waitFor();
                
                if (exitCode == 0 && isValid) {
                    System.out.println("APK签名验证通过！");
                    return true;
                } else {
                    System.err.println("APK签名验证失败！请检查签名配置。");
                    return false;
                }
            } else {
                // 如果apksigner不可用，使用jarsigner验证
                ProcessBuilder verifyBuilder = new ProcessBuilder(
                        "jarsigner", "-verify", "-verbose", "-certs", apkFile.getAbsolutePath());
                verifyBuilder.redirectErrorStream(true);
                
                Process verifyProcess = verifyBuilder.start();
                boolean isValid = false;
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(verifyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("jar verified") || line.contains("jar 已验证")) {
                            isValid = true;
                        }
                    }
                }
                
                int exitCode = verifyProcess.waitFor();
                
                if (exitCode == 0 && isValid) {
                    System.out.println("APK签名验证通过！");
                    return true;
                } else {
                    System.err.println("APK签名验证失败！请检查签名配置。");
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("验证签名时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 打印APK安装说明
     * @param apkFile APK文件
     */
    private void printInstallInstructions(File apkFile) {
        System.out.println("\n===== APK安装说明 =====");
        System.out.println("APK文件已生成: " + apkFile.getAbsolutePath());
        System.out.println("\n可通过以下方式安装到设备上:");
        
        // ADB安装指令
        System.out.println("\n1. 使用ADB命令安装 (需要开启USB调试):");
        System.out.println("   adb install -r \"" + apkFile.getAbsolutePath() + "\"");
        
        // 直接在设备上安装
        System.out.println("\n2. 直接在Android设备上安装:");
        System.out.println("   - 将APK文件传输到设备 (通过USB、蓝牙或网络)");
        System.out.println("   - 在设备上找到并点击APK文件");
        System.out.println("   - 如果提示'未知来源'，需要在设置中允许安装");
        
        // 常见安装问题
        System.out.println("\n如果安装失败，可能的原因:");
        System.out.println("- 应用签名不信任: 需要在设备上允许未知来源应用安装");
        System.out.println("- 版本兼容性: 确保应用的minSdkVersion与设备Android版本兼容");
        System.out.println("- 包名冲突: 设备上可能已安装相同包名的应用，尝试先卸载");
        System.out.println("- APK损坏: 检查APK文件是否完整、是否正确签名");
        
        // 使用ADB查看错误日志
        System.out.println("\n使用ADB查看安装错误:");
        System.out.println("adb logcat | grep PackageManager");
        
        System.out.println("\n=============================");
    }
} 