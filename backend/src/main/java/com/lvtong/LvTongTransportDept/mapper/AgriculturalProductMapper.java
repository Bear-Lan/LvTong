package com.lvtong.LvTongTransportDept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvtong.LvTongTransportDept.entity.AgriculturalProduct;

/**
 * 农产品品种表 Mapper
 * 用于 goods_type(product_code) → variety_name 查询
 */
public interface AgriculturalProductMapper extends BaseMapper<AgriculturalProduct> {
}
