package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stream")
@CrossOrigin
public class StreamController {

    private static final Map<Integer, ChannelInfo> CHANNEL_MAP = new ConcurrentHashMap<>();
    private static final int MEDIA_SERVER_PORT = 8889;

    static {
        CHANNEL_MAP.put(1, new ChannelInfo(1, "通道1", "车头相机", "front", "", "/channel_1"));
        CHANNEL_MAP.put(2, new ChannelInfo(2, "通道2", "车尾相机", "rear", "", "/channel_2"));
        CHANNEL_MAP.put(3, new ChannelInfo(3, "通道3", "车道相机", "lane", "", "/channel_3"));
        CHANNEL_MAP.put(4, new ChannelInfo(4, "通道4", "预约机", "appointment", "", "/channel_4"));
        CHANNEL_MAP.put(5, new ChannelInfo(5, "通道5", "球机", "ptz360", "", "/channel_5"));
    }

    private String getMediaServerUrl(HttpServletRequest request) {
        // 媒体服务器使用 HTTP 协议
        String host = request.getServerName();
        return "http://" + host + ":" + MEDIA_SERVER_PORT;
    }

    @GetMapping("/channels")
    public ApiResponse<List<ChannelInfo>> listChannels(HttpServletRequest request) {
        String mediaServerUrl = getMediaServerUrl(request);
        List<ChannelInfo> list = CHANNEL_MAP.keySet().stream()
            .sorted()
            .map(k -> {
                ChannelInfo info = CHANNEL_MAP.get(k);
                return new ChannelInfo(info.getChannel(), info.getName(), info.getDescription(),
                    info.getCameraType(), mediaServerUrl, info.getUrl());
            })
            .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @GetMapping("/url")
    public ApiResponse<StreamUrl> getStreamUrl(@RequestParam Integer channel, HttpServletRequest request) {
        if (channel == null || channel < 1 || channel > 5) {
            return ApiResponse.error("无效的通道号，范围1-5");
        }

        ChannelInfo channelInfo = CHANNEL_MAP.get(channel);
        if (channelInfo == null) {
            return ApiResponse.error("通道不存在: " + channel);
        }

        String mediaServerUrl = getMediaServerUrl(request);
        StreamUrl url = new StreamUrl();
        url.setChannel(channel);
        url.setChannelName(channelInfo.getName());
        url.setDescription(channelInfo.getDescription());
        url.setWebrtcUrl(mediaServerUrl + channelInfo.getUrl());

        return ApiResponse.success(url);
    }

    // 内部类
    public static class ChannelInfo {
        private Integer channel;
        private String name;
        private String description;
        private String cameraType;
        private String mediaServerUrl;
        private String url;

        public ChannelInfo() {}
        public ChannelInfo(Integer channel, String name, String description, String cameraType, String mediaServerUrl, String url) {
            this.channel = channel;
            this.name = name;
            this.description = description;
            this.cameraType = cameraType;
            this.mediaServerUrl = mediaServerUrl;
            this.url = url;
        }

        public Integer getChannel() { return channel; }
        public void setChannel(Integer channel) { this.channel = channel; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCameraType() { return cameraType; }
        public void setCameraType(String cameraType) { this.cameraType = cameraType; }
        public String getMediaServerUrl() { return mediaServerUrl; }
        public void setMediaServerUrl(String mediaServerUrl) { this.mediaServerUrl = mediaServerUrl; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class StreamUrl {
        private Integer channel;
        private String channelName;
        private String description;
        private String webrtcUrl;

        public Integer getChannel() { return channel; }
        public void setChannel(Integer channel) { this.channel = channel; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getWebrtcUrl() { return webrtcUrl; }
        public void setWebrtcUrl(String webrtcUrl) { this.webrtcUrl = webrtcUrl; }
    }
}
