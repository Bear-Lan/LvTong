package com.lvtong.LvTongTransportDept.service;

import com.lvtong.LvTongTransportDept.dto.ThreeLevelCheckResultDto;
import java.util.Map;

/**
 * 三级平台接收服务接口
 */
public interface ThreeLevelPlatformService {

    /**
     * 接收并保存三级平台上交的查验记录
     * @param dto 查验记录数据
     * @return 处理结果
     */
    Map<String, Object> receiveAndSave(ThreeLevelCheckResultDto dto);
}