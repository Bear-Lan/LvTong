package com.lvtong.LvTongTransportDept.config;

import com.lvtong.LvTongTransportDept.hksdk.HCNetSDK;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 海康威视 SDK 动态库加载配置
 * 支持通过配置文件指定库文件路径
 */
@Slf4j
@Configuration
public class HikLibConfig {

    @Value("${hiklib_path}")
    private String hikLibPath;

    private static HCNetSDK hcNetSDK;

    @Bean
    public HCNetSDK initHkSDK() {
        log.info("initHkSDK.hiklibPath=========> {}", hikLibPath);
        String os = System.getProperty("os.name");
        String actualPath = hikLibPath;

        if (StringUtils.isNotEmpty(os) && os.toLowerCase().startsWith("windows")) {
            if ("-1".equals(hikLibPath) || StringUtils.isEmpty(hikLibPath)) {
                // 未配置海康库文件路径，读取工程中的库文件
                String classPath = HikLibConfig.class.getResource("/").getPath();
                if (classPath != null) {
                    classPath = classPath.replaceAll("%20", " ");
                    // classPath 格式: /D:/path/to/classes 或 D:/path/to/classes
                    if (classPath.startsWith("/") && classPath.length() > 2) {
                        classPath = classPath.substring(1); // 去掉开头的 "/"
                    }
                    // static 目录在 classpath 下
                    actualPath = classPath + "static" + File.separator + "hikLib" + File.separator + "windows" + File.separator + "HCNetSDK.dll";
                } else {
                    // fallback: 使用当前工作目录
                    actualPath = System.getProperty("user.dir") + File.separator + "hikvision" + File.separator + "windows" + File.separator + "HCNetSDK.dll";
                }
            }
        }

        log.info("Loading HCNetSDK from: {}", actualPath);
        File dllFile = new File(actualPath);
        if (!dllFile.exists()) {
            log.error("HCNetSDK.dll not found at: {}", actualPath);
            throw new RuntimeException("HCNetSDK.dll not found at: " + actualPath);
        }

        hcNetSDK = (HCNetSDK) Native.loadLibrary(actualPath, HCNetSDK.class);
        log.info("HCNetSDK loaded successfully");
        return hcNetSDK;
    }

    public static HCNetSDK getInstance() {
        return hcNetSDK;
    }
}