package com.lvtong.LvTongTransportDept.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stream")
public class StreamConfig {

    /** 流媒体服务器地址（mediamtx） */
    private String mediaServer = "127.0.0.1:8889";

    /** 流协议 */
    private String protocol = "webrtc";

    public String getMediaServer() { return mediaServer; }
    public void setMediaServer(String mediaServer) { this.mediaServer = mediaServer; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    /**
     * 生成WebRTC播放地址
     * 格式: webrtc://127.0.0.1:8889/channel_1
     */
    public String generateWebrtcUrl(int channel) {
        return String.format("%s://%s/channel_%d", protocol, mediaServer, channel);
    }
}