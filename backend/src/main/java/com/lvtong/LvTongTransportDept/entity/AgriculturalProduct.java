package com.lvtong.LvTongTransportDept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 农产品品种实体
 *
 * 用于 goods_type(product_code) → variety_name 查询
 */
@TableName("agricultural_products")
public class AgriculturalProduct {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 产品类型 */
    @TableField("product_type")
    private String productType;

    /** 产品编码（对应 vehicle_inspections.goods_type） */
    @TableField("product_code")
    private String productCode;

    /** 产品类别 */
    @TableField("category")
    private String category;

    /** 品种名称（用于查验详情页显示） */
    @TableField("variety_name")
    private String varietyName;

    /** 别名/俗称（JSON 数组，如：["菠菜","波斯菜"]） */
    @TableField("aliases")
    private String aliases;

    // ---- Getter & Setter ----

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getVarietyName() { return varietyName; }
    public void setVarietyName(String varietyName) { this.varietyName = varietyName; }

    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }
}
