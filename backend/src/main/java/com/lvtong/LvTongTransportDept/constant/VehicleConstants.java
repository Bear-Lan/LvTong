package com.lvtong.LvTongTransportDept.constant;

import java.util.List;
import java.util.Map;

/**
 * 车辆查验业务常量类
 *
 * 【设计说明】
 * 集中管理所有查验相关的业务码与文本的映射关系，避免散落在各处。
 * 使用 switch-case 而非 Map，是为了：
 *   1. 编译期校验，key 拼写错误会直接报编译错误
 *   2. 性能更优（字节码层面直接跳转，无需查表）
 *   3. 代码更直观，可直接看到所有枚举值
 *
 * 【命名规范】
 * - RESULT_    ：查验结果状态（0=待查验, 1=合格, 2=不合格）
 * - STATUS_    ：状态标记（0=正常, -1=已删除，物理删除）
 * - REVIEW_    ：人工复核状态（0=未复核, 1=已复核）
 * - SUBMIT_    ：报送交通局状态（0=未提交, 1=已提交, -1=提交失败）
 *
 * 【线程安全】
 * 此类仅包含纯静态方法和常量，无状态，可并发访问，无需加锁。
 */
public class VehicleConstants {

    /**
     * 私有构造函数，防止外部实例化
     * 此类为工具类，所有方法均为静态方法，无需创建实例。
     */
    private VehicleConstants() {}

    // ================================================================
    // 查验结果（result_status）
    // ================================================================
    /** 待查验 - 车辆尚未完成查验流程 */
    public static final int RESULT_PENDING = 0;
    /** 合格 - 车辆符合绿通减免条件 */
    public static final int RESULT_PASS    = 1;
    /** 不合格 - 车辆不符合绿通减免条件 */
    public static final int RESULT_FAIL    = 2;

    /**
     * 将查验结果码转换为可读文本
     *
     * @param status 查验结果码（0=待查验, 1=合格, 2=不合格）
     * @return 中文可读文本，null 时返回 "-"
     */
    public static String getResultStatusText(Integer status) {
        if (status == null) return "-";
        switch (status) {
            case RESULT_PENDING: return "待查验";   // 0：尚未开始查验
            case RESULT_PASS:    return "合格";     // 1：符合条件
            case RESULT_FAIL:    return "不合格";    // 2：不符合条件
            default:             return "未知";     // 兜底，防止脏数据
        }
    }

    // ================================================================
    // 状态标记（status）
    // 注意：此处 status 与 vehicle_inspections.status 字段对应（0=正常, -1=已删除）。
    // ================================================================
    /** 正常 */
    public static final int STATUS_NORMAL  = 0;
    /** 已删除（物理删除） */
    public static final int STATUS_DELETED = -1;

    /**
     * 将状态码转换为可读文本
     *
     * @param status 状态码（0=正常, -1=已删除）
     * @return 中文可读文本，null 时返回 "-"
     */
    public static String getStatusText(Integer status) {
        if (status == null) return "-";
        switch (status) {
            case STATUS_NORMAL:  return "正常";     // 0：记录有效
            case STATUS_DELETED: return "已删除";  // -1：记录已软删除
            default:             return "未知";
        }
    }

    // ================================================================
    // 人工复核状态（manual_review_state）
    // 查验结果为"不合格"时，可能需要人工复核确认。
    // ================================================================
    /** 未复核 - 不合格记录尚未经过人工复核 */
    public static final int REVIEW_PENDING = 0;
    /** 已复核 - 已由人工确认不合格原因 */
    public static final int REVIEW_DONE    = 1;

    /**
     * 将人工复核状态码转换为可读文本
     *
     * @param state 复核状态（0=未复核, 1=已复核）
     * @return 中文可读文本，null 时返回 "-"
     */
    public static String getManualReviewText(Integer state) {
        if (state == null) return "-";
        // 只有两种状态，非 1 即为"未复核"
        return state == REVIEW_DONE ? "已复核" : "未复核";
    }

    // ================================================================
    // 报送交通局状态（to_transportdept_state）
    // 查验结果为"不合格"的记录，需要按规定报送当地交通局。
    // ================================================================
    /** 未提交 - 尚未向交通局提交记录 */
    public static final int SUBMIT_PENDING = 0;
    /** 已提交 - 已成功提交至交通局平台 */
    public static final int SUBMIT_DONE    = 1;
    /** 提交失败 - 提交过程中发生错误，需重试 */
    public static final int SUBMIT_FAIL    = -1;

    /**
     * 将交通局报送状态码转换为可读文本
     *
     * @param state 报送状态（0=未提交, 1=已提交, -1=提交失败）
     * @return 中文可读文本，null 时返回 "-"
     */
    public static String getTransportDeptStateText(Integer state) {
        if (state == null) return "-";
        switch (state) {
            case SUBMIT_DONE:    return "已提交";     // 1：成功
            case SUBMIT_FAIL:    return "提交失败";    // -1：失败
            default:             return "未提交";     // 0 和其他值均为"未提交"
        }
    }

    // ================================================================
    // 车种分类（vehicleClass）
    // 数据库中无此字段，由 vehicle_type 推导而来。
    // 用于区分"绿通车"和"联合收割机"两大类别。
    // ================================================================
    /** 绿通车 - 运输鲜活农产品的货车，享受绿通减免政策 */
    public static final int CLASS_GREEN       = 2;
    /** 联合收割机 - 跨区作业的联合收割机，享受免费政策 */
    public static final int CLASS_HARVESTER   = 3;

    /**
     * 将车种码转换为可读文本
     *
     * 注意：此方法目前由 Controller 层根据 vehicle_type 推导，
     * 不依赖数据库中的 vehicle_class 字段。
     *
     * @param vehicleClass 车种码（2=绿通车, 3=联合收割机）
     * @return 中文可读文本，null 时返回 "-"
     */
    public static String getVehicleClassText(Integer vehicleClass) {
        if (vehicleClass == null) return "-";
        switch (vehicleClass) {
            case CLASS_GREEN:     return "绿通车";      // 2
            case CLASS_HARVESTER: return "联合收割机";  // 3
            default:              return "其他(" + vehicleClass + ")"; // 未知类型，显示原始值
        }
    }

    // ================================================================
    // 车辆类型（vehicle_type）
    // 数据来源：用户输入 / 前端下拉选择
    // ================================================================

    /**
     * 将车型码转换为标准车型名称
     *
     * @param type 车型码字符串（"11"~"16" 对应一型~六型货车）
     * @return 中文车型名称，null/空时返回 "-"
     */
    public static String getVehicleTypeText(String type) {
        if (type == null || type.isEmpty()) return "-";
        switch (type) {
            case "11": return "一型货车";  // 载重≤2吨
            case "12": return "二型货车";  // 载重2~5吨
            case "13": return "三型货车";  // 载重5~10吨
            case "14": return "四型货车";  // 载重10~15吨
            case "15": return "五型货车";  // 载重15~20吨
            case "16": return "六型货车";  // 载重>20吨
            default:   return "未知(" + type + ")"; // 兜底
        }
    }

    // ================================================================
    // 车牌颜色（passcode_vehicle_color_name）
    // 用于 ETC 通行卡中记录的车辆颜色，与实际车牌颜色对应。
    // 取值来源：前端下拉选择 / ETC 系统传入
    // ================================================================

    /**
     * 将车牌颜色码转换为中文颜色名称
     *
     * @param color 颜色码字符串（"0"~"12"）
     * @return 中文颜色名称，null/空时返回 "-"
     *
     * 颜色码对照表：
     *   0=蓝色（最常见小型车）  1=黄色（大型车/教练车）
     *   2=黑色（港澳牌/外资）   3=白色（警车/临时牌）
     *   4=渐变绿色（新能源车牌） 5=黄绿双拼（香港车牌）
     *   6=蓝白渐变色           7=临时牌照
     *   11=绿色（早期新能源）   12=红色（临时牌/使馆牌）
     */
    public static String getVehicleColorText(String color) {
        if (color == null || color.isEmpty()) return "-";
        switch (color) {
            case "0":  return "蓝色";       // 0：蓝牌
            case "1":  return "黄色";       // 1：黄牌
            case "2":  return "黑色";       // 2：黑牌
            case "3":  return "白色";       // 3：白牌
            case "4":  return "渐变绿色";   // 4
            case "5":  return "黄绿双拼色"; // 5
            case "6":  return "蓝白渐变色"; // 6
            case "7":  return "临时牌照";   // 7
            case "11": return "绿色";       // 11
            case "12": return "红色";       // 12
            default:   return "其他(" + color + ")"; // 未知
        }
    }

    // ================================================================
    // 货箱类型（vehicle_containertype）
    // 用于区分货车车厢结构，影响货物装载方式和查验重点。
    // ================================================================

    /**
     * 将货箱类型码转换为标准货箱类型名称
     *
     * @param containerType 货箱类型码（"1", "2.1", "2.2", "3.1", "4.1", "5.1"）
     * @return 中文货箱类型名称，null/空时返回原始值
     *
     * 类型对照表：
     *   1    = 罐式货车（密封容器，液态/散装货物）
     *   2.1  = 敞篷货车-平板（无侧板，适合大型机械）
     *   2.2  = 敞篷货车-栅栏（有栅栏挡板，适合牲畜/轻抛货）
     *   3.1  = 普通货车-篷布包裹（最常见，用篷布遮盖货物）
     *   4.1  = 厢式货车-封闭（全封闭箱体，防雨防盗）
     *   5.1  = 特殊结构-水箱式（运输水产品，活鱼/活蟹等）
     */
    public static String getContainerTypeText(String containerType) {
        if (containerType == null || containerType.isEmpty()) return "-";
        switch (containerType) {
            case "1":   return "罐式货车";
            case "2.1": return "敞篷货车(平板)";
            case "2.2": return "敞篷货车(栅栏)";
            case "3.1": return "普通货车(篷布包裹式)";
            case "4.1": return "厢式货车(封闭货车)";
            case "5.1": return "特殊结构货车(水箱式)";
            default:    return containerType; // 未知类型时原样返回，便于排查
        }
    }

    // ================================================================
    // 通行介质类型（passcode_media_type）
    // 车辆进入高速时使用的通行介质，影响计费方式和查验流程。
    // ================================================================

    /**
     * 将通行介质码转换为标准介质名称
     *
     * @param type 介质码字符串（"0", "1", "2", "3", "4", "9" 或 "OBU", "CPC" 等）
     * @return 中文介质名称，null/空时返回 "-"
     *
     * 介质对照表：
     *   1/0/OBU = OBU（电子标签，车载单元，自动扣费，最快）
     *   2/CPU   = CPC卡（高速公路复合通行卡，入口领卡出口交卡）
     *   3/PAPER = 纸券（早期/特殊情况，入口手工撕券）
     *   4       = M1卡（早期IC卡，已逐步淘汰）
     *   9       = 无通行介质（特殊情况，如紧急救援车辆）
     */
    public static String getMediaTypeText(String type) {
        if (type == null || type.isEmpty()) return "-";
        switch (type) {
            case "1":    return "OBU";        // 电子标签
            case "2":    return "CPC卡";      // 复合通行卡
            case "3":    return "纸券";       // 纸质通行券
            case "4":    return "M1卡";       // M1逻辑加密卡
            case "9":    return "无通行介质";  // 9：特种车辆
            case "0":    return "OBU";        // 0 也表示 OBU（兼容旧数据）
            case "CPU":  return "CPU卡";      // 大写别名
            case "OBU":  return "OBU";        // 全大写别名
            case "PAPER": return "纸券";       // 全大写别名
            default:     return type;         // 未知，原样返回
        }
    }

    // ================================================================
    // 交易支付方式（passcode_trans_pay_type）
    // ================================================================

    /**
     * 将交易支付方式码转换为可读文本
     *
     * @param type 支付方式码
     *             1=出口ETC通行，2=出口ETC刷卡，其他=未知
     * @return 中文支付方式名称，null/空时返回 "-"
     */
    public static String getTransPayTypeText(String type) {
        if (type == null || type.isEmpty()) return "-";
        switch (type) {
            case "1": return "出口ETC通行";
            case "2": return "出口ETC刷卡";
            default:  return "未知";
        }
    }

    // ================================================================
    // 车辆状态标识（passcode_vehicle_sign）
    // ================================================================

    /**
     * 将车辆状态标识码转换为可读文本
     *
     * @param sign 状态标识码（十六进制字符串，如 "0x02", "0x03", "0xFF"）
     *             0x02=绿通车，0x03=收割机，0xFF=默认，其他=未知
     * @return 中文状态名称，null/空时返回 "-"
     */
    public static String getVehicleSignText(String sign) {
        if (sign == null || sign.isEmpty()) return "-";
        switch (sign.toUpperCase()) {
            case "0X02": return "绿通车";
            case "0X03": return "收割机";
            case "0XFF": return "默认";
            default:     return "未知";
        }
    }

    // ================================================================
    // 不合格类型（nopass_type）
    // 仅在 result_status = 2（不合格）时填写，表明不合格的具体原因。
    // 分为两大类：绿通不合格（11-26）和联合收割机不合格（31-42）。
    //
    // 【为什么用 Integer 入参而不是 String？】
    // 数据库中 nopass_type 字段类型为 Integer。
    // 直接用 Integer 入参可以避免拆箱（unboxing）开销，且编译期类型更安全。
    // 方法内部通过 String.valueOf() 统一转为字符串做 switch 匹配。
    // ================================================================

    /**
     * 将不合格类型码转换为可读文本
     *
     * @param nopassType 不合格类型码（Integer 类型）
     *                    - 绿通类：11~26
     *                    - 收割机类：31~42
     * @return 中文不合格原因，null 时返回 "-"
     *
     * 【绿通不合格类型说明】
     *   11=车货总质量超限（超过行驶证核定总质量）
     *   12=外廓尺寸超限（长/宽/高超过行驶证标注）
     *   13=货物非《目录》内（运输的货物不在免费目录中）
     *   14=货物属深加工产品（初级农产品才免费，深加工不免费）
     *   15=货物冷冻发硬/腐烂/变质（不鲜活）
     *   18=未达80%装载（装载率或容积率不足80%）
     *   19=混装非鲜活农产品（混装了目录外货物）
     *   20=混装目录外鲜活农产品超20%（可混装但超比例）
     *   21=假冒绿通（最严重，试图偷逃通行费）
     *   22=未提供行驶证原件（无法核实载质量）
     *   23=电子证件无法核定载质量
     *   24=行驶证过期
     *   25=行驶证标注仅可运送不可拆解物体
     *   26=动物检疫合格证明问题
     *
     * 【联合收割机不合格类型说明】
     *   31=《作业证》无效（跨区作业证过期/伪造/信息不符）
     *   33=车货总质量超限
     *   34=外廓尺寸超限
     *   35=收割机未悬挂正式号牌
     *   38=混装其他物品（收割机只能运输收割机本身及配件）
     *   39=无《作业证》（未办理跨区作业证）
     *   40=未提供行驶证原件
     *   41=提供的电子证件无法核定载质量
     *   42=行驶证过期
     */
    public static String getNopassTypeText(Integer nopassType) {
        if (nopassType == null) return "-";
        // Integer → String 统一转换，兼容数据库存整数或字符串的情况
        String type = String.valueOf(nopassType);
        switch (type) {
            // ---- 绿通不合格（11-26，含已停用）----
            case "11": return "车货总质量超限";
            case "12": return "外廓尺寸超限";
            case "13": return "货物非《目录》内";
            case "14": return "货物属深加工产品";
            case "15": return "货物冷冻发硬、腐烂、变质";
            case "16": return "未安装ETC（停用）";
            case "17": return "已安装ETC但入口未正常使用（停用）";
            case "18": return "未达核定载质量和车厢容积80%以上";
            case "19": return "混装非鲜活农产品";
            case "20": return "混装《目录》外鲜活农产品超20%";
            case "21": return "假冒绿通";
            case "22": return "未提供行驶证原件";
            case "23": return "提供的电子证件无法核定载质量";
            case "24": return "行驶证过期";
            case "25": return "行驶证标注仅可运送不可拆解物体";
            case "26": return "动物检疫合格证明问题";
            // ---- 联合收割机不合格（31-42，含已停用）----
            case "31": return "《作业证》无效";
            case "32": return "《作业证》信息与实际不符（停用）";
            case "33": return "车货总质量超限";
            case "34": return "外廓尺寸超限";
            case "35": return "收割机未悬挂正式号牌";
            case "36": return "未安装ETC（停用）";
            case "37": return "已安装ETC但入口未正常使用（停用）";
            case "38": return "混装其他物品";
            case "39": return "无《作业证》";
            case "40": return "未提供行驶证原件";
            case "41": return "提供的电子证件无法核定载质量";
            case "42": return "行驶证过期";
            // ---- 兜底 ----
            default: return type; // 未知类型时原样返回编号，便于人工核对
        }
    }

    /**
     * 获取所有不合格类型的映射列表（用于前端下拉选项）
     * @return List<Map> 每个元素包含 value 和 label
     */
    public static List<Map<String, Object>> getNopassTypeOptions() {
        List<Map<String, Object>> options = new java.util.ArrayList<>();
        // 绿通类
        options.add(Map.of("value", 11, "label", "车货总质量超限"));
        options.add(Map.of("value", 12, "label", "外廓尺寸超限"));
        options.add(Map.of("value", 13, "label", "货物非《目录》内"));
        options.add(Map.of("value", 14, "label", "货物属深加工产品"));
        options.add(Map.of("value", 15, "label", "货物冷冻发硬、腐烂、变质"));
        options.add(Map.of("value", 18, "label", "未达核定载质量和车厢容积80%以上"));
        options.add(Map.of("value", 19, "label", "混装非鲜活农产品"));
        options.add(Map.of("value", 20, "label", "混装《目录》外鲜活农产品超20%"));
        options.add(Map.of("value", 21, "label", "假冒绿通"));
        options.add(Map.of("value", 22, "label", "未提供行驶证原件"));
        options.add(Map.of("value", 23, "label", "提供的电子证件无法核定载质量"));
        options.add(Map.of("value", 24, "label", "行驶证过期"));
        options.add(Map.of("value", 25, "label", "行驶证标注仅可运送不可拆解物体"));
        options.add(Map.of("value", 26, "label", "动物检疫合格证明问题"));
        // 收割机类
        options.add(Map.of("value", 31, "label", "《作业证》无效"));
        options.add(Map.of("value", 33, "label", "车货总质量超限"));
        options.add(Map.of("value", 34, "label", "外廓尺寸超限"));
        options.add(Map.of("value", 35, "label", "收割机未悬挂正式号牌"));
        options.add(Map.of("value", 38, "label", "混装其他物品"));
        options.add(Map.of("value", 39, "label", "无《作业证》"));
        options.add(Map.of("value", 40, "label", "未提供行驶证原件"));
        options.add(Map.of("value", 41, "label", "提供的电子证件无法核定载质量"));
        options.add(Map.of("value", 42, "label", "行驶证过期"));
        return options;
    }
}
