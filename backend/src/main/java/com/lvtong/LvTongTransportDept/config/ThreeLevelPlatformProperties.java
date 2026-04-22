package com.lvtong.LvTongTransportDept.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 三级平台配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "three-level-platform")
public class ThreeLevelPlatformProperties {

    /** 三级平台 HTTPS 接收地址 */
    private String httpsUrl;

    /** 客户端 ID */
    private String clientId;

    /** 客户端密钥 */
    private String clientKey;

    /** 本地图片存储目录 */
    private String localDir;
}