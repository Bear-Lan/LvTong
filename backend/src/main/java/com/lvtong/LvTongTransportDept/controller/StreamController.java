package com.lvtong.LvTongTransportDept.controller;

import com.lvtong.LvTongTransportDept.config.StreamConfig;
import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/stream")
@CrossOrigin
public class StreamController {

    @Autowired
    private StreamConfig streamConfig;

    private static final Map<Integer, ChannelInfo> CHANNEL_MAP = new ConcurrentHashMap<>();
    static {
        CHANNEL_MAP.put(1, new ChannelInfo(1, "通道1", "车头相机"));
        CHANNEL_MAP.put(2, new ChannelInfo(2, "通道2", "车尾相机"));
        CHANNEL_MAP.put(3, new ChannelInfo(3, "通道3", "车道相机"));
        CHANNEL_MAP.put(4, new ChannelInfo(4, "通道4", "预约机"));
        CHANNEL_MAP.put(5, new ChannelInfo(5, "通道5", "球机"));
    }

    @GetMapping("/channels")
    public ApiResponse<List<ChannelInfo>> listChannels() {
        return ApiResponse.success(new ArrayList<>(CHANNEL_MAP.values()));
    }

    @GetMapping("/url")
    public ApiResponse<StreamUrl> getStreamUrl(@RequestParam Integer channel) {
        if (channel == null || channel < 1 || channel > 5) {
            return ApiResponse.error("无效的通道号，范围1-5");
        }

        ChannelInfo channelInfo = CHANNEL_MAP.get(channel);
        if (channelInfo == null) {
            return ApiResponse.error("通道不存在: " + channel);
        }

        StreamUrl url = new StreamUrl();
        url.setChannel(channel);
        url.setChannelName(channelInfo.getName());
        url.setDescription(channelInfo.getDescription());
        url.setWebrtcUrl(streamConfig.generateWebrtcUrl(channel));

        return ApiResponse.success(url);
    }

    // 内部类
    public static class ChannelInfo {
        private Integer channel;
        private String name;
        private String description;

        public ChannelInfo() {}
        public ChannelInfo(Integer channel, String name, String description) {
            this.channel = channel;
            this.name = name;
            this.description = description;
        }

        public Integer getChannel() { return channel; }
        public void setChannel(Integer channel) { this.channel = channel; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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