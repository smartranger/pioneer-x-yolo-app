package com.pioneer.appbuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Gradle文件生成器
 * 用于处理项目的build.gradle文件
 */
public class GradleFileGenerator {
    
    private final AppConfig config;
    
    public GradleFileGenerator(AppConfig config) {
        this.config = config;
    }
    
    /**
     * 更新app/build.gradle文件
     * @param filePath 文件路径
     * @throws IOException 文件操作错误
     */
    public void updateAppBuildGradle(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("找不到build.gradle文件: " + filePath);
        }
        
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        
        // 更新应用ID和包名
        content = content.replaceAll("namespace \"[^\"]+\"", "namespace \"" + config.packageName + "\"");
        content = content.replaceAll("applicationId \"[^\"]+\"", "applicationId \"" + config.packageName + "\"");
        
        // 添加版本信息
        if (!content.contains("versionCode")) {
            content = content.replace("defaultConfig {", 
                    "defaultConfig {\n        versionCode " + config.versionCode + 
                    "\n        versionName \"" + config.versionName + "\"");
        } else {
            content = content.replaceAll("versionCode \\d+", "versionCode " + config.versionCode);
            content = content.replaceAll("versionName \"[^\"]+\"", "versionName \"" + config.versionName + "\"");
        }
        
        // 添加签名配置
        if (!content.contains("signingConfigs") && isSigningConfigValid()) {
            String signingConfig = generateSigningConfigBlock();
            content = content.replace("android {", "android {\n    " + signingConfig);
            
            // 应用签名配置
            content = content.replace("buildTypes {", 
                    "buildTypes {\n        release {\n            signingConfig signingConfigs.release\n        }");
        }
        
        // 禁用Lint检查，避免元空间内存不足
        if (!content.contains("lintOptions")) {
            content = content.replace("android {", 
                    "android {\n    lintOptions {\n        checkReleaseBuilds false\n        abortOnError false\n    }");
        }
        
        // 增加JVM内存，防止内存不足
        if (!content.contains("org.gradle.jvmargs")) {
            // 在文件末尾添加gradle.properties设置
            content += "\n\n// 增加JVM内存配置\nandroid.buildTypes.release.javaCompileOptions.annotationProcessorOptions.arguments = [\n" +
                       "    \"room.incremental\":\"true\",\n" +
                       "    \"room.expandProjection\":\"true\",\n" +
                       "    \"room.schemaLocation\":\"$projectDir/schemas\"\n" +
                       "]\n";
        }
        
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }
    
    /**
     * 生成签名配置块
     * @return 签名配置Gradle代码
     */
    private String generateSigningConfigBlock() {
        return "signingConfigs {\n" +
                "        release {\n" +
                "            storeFile file('" + config.keystoreConfig.keystorePath + "')\n" +
                "            storePassword '" + config.keystoreConfig.keystorePassword + "'\n" +
                "            keyAlias '" + config.keystoreConfig.keyAlias + "'\n" +
                "            keyPassword '" + config.keystoreConfig.keyPassword + "'\n" +
                "        }\n" +
                "    }\n";
    }
    
    /**
     * 检查签名配置是否有效
     * @return 是否有效
     */
    private boolean isSigningConfigValid() {
        return config.keystoreConfig != null && 
                config.keystoreConfig.keystorePath != null && 
                !config.keystoreConfig.keystorePath.isEmpty() && 
                new File(config.keystoreConfig.keystorePath).exists();
    }
} 