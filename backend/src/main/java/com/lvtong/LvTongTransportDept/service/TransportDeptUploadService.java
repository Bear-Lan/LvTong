package com.lvtong.LvTongTransportDept.service;

import java.util.List;
import java.util.Map;

/**
 * 交通局上报服务接口
 *
 * @see com.lvtong.LvTongTransportDept.service.impl.TransportDeptUploadServiceImpl
 */
public interface TransportDeptUploadService {

    /**
     * 上报单条查验记录到交通局平台（排除指定图片）
     *
     * @param id 查验记录 ID
     * @param excludePhotoTypes 要排除的图片类型ID列表，如 [11,12,13]
     * @return 上报结果（success / code / msg）
     */
    Map<String, Object> uploadSingle(Integer id, List<String> excludePhotoTypes);

}
