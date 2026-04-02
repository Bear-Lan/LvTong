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
     * 上报单条查验记录到交通局平台
     *
     * @param id 查验记录 ID
     * @return 上报结果（success / code / msg）
     */
    Map<String, Object> uploadSingle(Integer id);

    /**
     * 批量上报查验记录
     *
     * @param ids 查验记录 ID 列表
     * @return 批量上报汇总结果
     */
    Map<String, Object> uploadBatch(List<Integer> ids);
}
