package com.lvtong.LvTongTransportDept.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI识别服务接口
 *
 * @see com.lvtong.LvTongTransportDept.service.impl.AIDetectionServiceImpl
 */
public interface AIDetectionService {

    /**
     * 车辆照片识别
     * 识别车厢类型、车轮数量
     *
     * @param file 车辆图片文件
     * @return 识别结果，包含 wheel_count 和 cratetype
     */
    Map<String, Object> detectVehicle(MultipartFile file);

    /**
     * 货物类型识别
     * 识别货物类型
     *
     * @param file 货物图片文件
     * @return 识别结果，包含过滤后的 real_object_data
     */
    Map<String, Object> detectGoods(MultipartFile file);

    /**
     * 行驶证识别
     * 识别行驶证正反面信息
     *
     * @param file 行驶证图片文件
     * @return 行驶证信息JSON
     */
    Map<String, Object> detectDriverLicense(MultipartFile file);

    /**
     * 车轴识别
     * 识别车轴数量和位置
     *
     * @param file 车轴图片文件
     * @return 识别结果，包含 wheel_count 和 data 数组
     */
    Map<String, Object> detectAxle(MultipartFile file);

    /**
     * 车厢识别
     * 识别车厢类型
     *
     * @param file 车厢图片文件
     * @return 识别结果，包含车厢类型中文描述
     */
    Map<String, Object> detectCarriage(MultipartFile file);
}