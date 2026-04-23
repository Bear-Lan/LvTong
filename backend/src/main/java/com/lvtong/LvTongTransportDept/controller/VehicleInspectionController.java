package com.lvtong.LvTongTransportDept.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lvtong.LvTongTransportDept.constant.VehicleConstants;
import com.lvtong.LvTongTransportDept.dto.ApiResponse;
import com.lvtong.LvTongTransportDept.entity.AgriculturalProduct;
import com.lvtong.LvTongTransportDept.entity.VehicleInspection;
import com.lvtong.LvTongTransportDept.exception.BusinessException;
import com.lvtong.LvTongTransportDept.mapper.AgriculturalProductMapper;
import com.lvtong.LvTongTransportDept.service.VehicleInspectionService;
import com.lvtong.LvTongTransportDept.utils.ImageWatermarkUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆查验记录接口层（Controller）
 * 【RESTful 风格说明】
 * - GET    /api/inspection/list    → 查询列表（支持分页+多条件筛选）
 * - GET    /api/inspection/{id}     → 查询单条
 * - POST   /api/inspection          → 新增
 * - PUT    /api/inspection/{id}    → 更新
 * @see VehicleInspectionService
 * @see VehicleConstants
 */
@RestController
@RequestMapping("/api/inspection")
@Tag(name = "车辆查验", description = "车辆查验记录管理接口")
public class VehicleInspectionController {

    @Autowired
    private VehicleInspectionService inspectionService;

    @Autowired
    private AgriculturalProductMapper agriculturalProductMapper;

    // ================================================================
    // 【查询接口】
    // ================================================================

    /**
     * 查询查验列表（支持多条件筛选 + 分页）
     *
     * 【迁移说明】
     * 原使用 Pageable 参数 + JPA Sort → 直接传 page + pageSize，排序由 Service 层控制。
     * MP 页码从 1 开始，与前端 page 参数一致，无需减 1。
     *
     * @return 统一响应结构，data 中含 records / total / page / pageSize
     */
    @GetMapping("/list")
    @Operation(
        summary = "获取查验列表",
        description = "获取车辆查验记录，支持多条件筛选和分页，按查验时间倒序排列"
    )
    public ApiResponse<Map<String, Object>> getList(
            @Parameter(description = "车牌号（模糊查询）")
            @RequestParam(required = false) String plateNumber,

            @Parameter(description = "司机电话（精确查询）")
            @RequestParam(required = false) String driverPhone,

            @Parameter(description = "操作员（精确查询）")
            @RequestParam(required = false) String operatorName,

            @Parameter(description = "核验员电话（精确查询）")
            @RequestParam(required = false) String reviewerPhone,

            @Parameter(description = "开始时间（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,

            @Parameter(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,

            @Parameter(description = "查验结果: 1=合格, 2=不合格")
            @RequestParam(required = false) Integer resultStatus,

            @Parameter(description = "复核结果: 0=待审核, 1=审核通过, 2=审核未通过")
            @RequestParam(required = false) Integer manualReviewState,

            @Parameter(description = "上传状态: 0=未上传, 1=成功, -1=失败")
            @RequestParam(required = false) Integer toTransportdeptState,

            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") int pageSize) {

        // 调用 Service 层执行多条件查询（MP 页码从 1 开始，无需减 1）
        IPage<VehicleInspection> result = inspectionService.searchWithConditions(
                plateNumber, driverPhone, operatorName, reviewerPhone,
                startTime, endTime, resultStatus, manualReviewState, toTransportdeptState,
                page, pageSize);

        // 转换为前端所需格式（含转换后的文本字段）
        List<Map<String, Object>> data = result.getRecords().stream()
                .map(this::convertToMap)
                .toList();

        // 构建分页元数据（IPage 直接提供这些字段）
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", data);
        pageData.put("total", result.getTotal());
        pageData.put("page", result.getCurrent());
        pageData.put("pageSize", result.getSize());

        return ApiResponse.success(pageData);
    }

    /**
     * 导出查询（全量数据，不分页）
     * 返回符合筛选条件的全部数据，用于Excel导出
     */
    @GetMapping("/export")
    @Operation(
        summary = "导出查询",
        description = "获取车辆查验记录（全量），支持多条件筛选，用于Excel导出"
    )
    public ApiResponse<List<Map<String, Object>>> getExportList(
            @Parameter(description = "车牌号（模糊查询）")
            @RequestParam(required = false) String plateNumber,

            @Parameter(description = "司机电话（精确查询）")
            @RequestParam(required = false) String driverPhone,

            @Parameter(description = "核验员电话（精确查询）")
            @RequestParam(required = false) String reviewerPhone,

            @Parameter(description = "开始时间（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,

            @Parameter(description = "结束时间（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,

            @Parameter(description = "查验结果: 1=合格, 2=不合格")
            @RequestParam(required = false) Integer resultStatus,

            @Parameter(description = "复核结果: 0=待审核, 1=审核通过, 2=审核未通过")
            @RequestParam(required = false) Integer manualReviewState,

            @Parameter(description = "上传状态: 0=未上传, 1=成功, -1=失败")
            @RequestParam(required = false) Integer toTransportdeptState) {

        List<VehicleInspection> list = inspectionService.searchForExport(
                plateNumber, driverPhone, reviewerPhone,
                startTime, endTime, resultStatus, manualReviewState, toTransportdeptState);

        // 转换为Map格式
        List<Map<String, Object>> data = list.stream().map(this::convertToMap).toList();

        return ApiResponse.success(data);
    }

    /**
     * 获取农产品品种列表（用于货物类型下拉选择）
     * 返回所有品种数据，前端自行构建筛选条件
     */
    @GetMapping("/products")
    @Operation(summary = "获取农产品品种列表", description = "返回所有农产品品种，用于货物类型下拉选择")
    public ApiResponse<Map<String, Object>> getProducts() {
        List<AgriculturalProduct> products = agriculturalProductMapper.selectList(null);

        // 去重后的产品大类（按字母顺序）
        List<String> productTypes = products.stream()
                .map(AgriculturalProduct::getProductType)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted()
                .toList();

        // 去重后的具体类别
        List<String> categories = products.stream()
                .map(AgriculturalProduct::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();

        // 完整品种列表
        List<Map<String, Object>> varieties = products.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("productCode", p.getProductCode());
            item.put("varietyName", p.getVarietyName());
            item.put("varietyNamePinyin", p.getVarietyNamePinyin());
            item.put("aliasesPinyin", p.getAliasesPinyin());
            item.put("category", p.getCategory());
            item.put("productType", p.getProductType());
            item.put("aliases", p.getAliases());
            return item;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("productTypes", productTypes);
        data.put("categories", categories);
        data.put("varieties", varieties);
        return ApiResponse.success(data);
    }

    /**
     * 获取不合格类型选项列表（用于下拉选择）
     * 返回所有不合格类型映射，前端直接渲染下拉选项
     */
    @GetMapping("/nopass-types")
    @Operation(summary = "获取不合格类型选项", description = "返回所有不合格类型映射，用于下拉选择")
    public ApiResponse<List<Map<String, Object>>> getNopassTypes() {
        List<Map<String, Object>> options = VehicleConstants.getNopassTypeOptions();
        return ApiResponse.success(options);
    }

    /**
     * 根据 ID 查询单条查验记录
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取查验详情", description = "根据ID获取单条查验记录")
    public ApiResponse<Map<String, Object>> getById(
            @Parameter(description = "查验记录ID") @PathVariable Integer id) {
        VehicleInspection record = inspectionService.getById(id);
        if (record == null) {
            return ApiResponse.error("查验记录不存在");
        }
        return ApiResponse.success(convertToMap(record));
    }

    // ================================================================
    // 【写接口】
    // ================================================================

    /**
     * 新增查验记录
     */
    @PostMapping
    @Operation(summary = "创建查验记录", description = "新增一条车辆查验记录")
    public ApiResponse<Map<String, Object>> create(
            @RequestBody VehicleInspection inspection,
            HttpServletRequest request) {
        // 操作员姓名从 JWT Token 获取，忽略前端提交的值，防止伪造
        String operatorName = (String) request.getAttribute("username");
        if (operatorName == null || operatorName.isBlank()) {
            throw new BusinessException("无法获取操作员信息，请重新登录");
        }
        inspection.setOperatorName(operatorName);

        VehicleInspection created = inspectionService.create(inspection);
        return ApiResponse.success("创建成功", convertToMap(created));
    }

    /**
     * 更新查验记录（部分更新）
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新查验记录",
        description = "更新车辆查验记录信息（仅更新非空字段，支持部分更新）"
    )
    public ApiResponse<Map<String, Object>> update(
            @PathVariable Integer id,
            @RequestBody VehicleInspection inspection) {

        // 先获取更新前的数据
        VehicleInspection oldData = inspectionService.getById(id);
        if (oldData == null) {
            return ApiResponse.error("记录不存在");
        }

        // 更新数据库
        VehicleInspection updated = inspectionService.update(id, inspection);

        // 检查7个水印字段是否有变更
        boolean watermarkChanged = checkWatermarkFieldsChanged(oldData, updated);

        // 如果有变更且有透明图，重新绘制水印
        if (watermarkChanged && updated.getTransparentImagePath() != null) {
            String imagePath = updated.getTransparentImagePath();
            // 获取文本值
            String plateNumber = updated.getPlateNumber();
            String plateColor = VehicleConstants.getVehicleColorText(updated.getPasscodeVehicleColorName());
            String vehicleTypeText = VehicleConstants.getVehicleTypeText(updated.getVehicleType());
            String containerTypeText = VehicleConstants.getContainerTypeText(updated.getVehicleContainertype());
            String goodsTypeText = resolveGoodsTypeName(updated.getGoodsType());
            String detectDate = updated.getPasscodeExTime();

            ImageWatermarkUtil.drawWatermarkAndOverwrite(
                    imagePath, plateNumber, plateColor, vehicleTypeText,
                    containerTypeText, goodsTypeText, detectDate);
        }

        return ApiResponse.success("更新成功", convertToMap(updated));
    }

    /**
     * 检查7个水印字段是否变更
     */
    private boolean checkWatermarkFieldsChanged(VehicleInspection oldData, VehicleInspection newData) {
        return !equalsOrBothNull(oldData.getPlateNumber(), newData.getPlateNumber())
                || !equalsOrBothNull(oldData.getPlateNumberGc(), newData.getPlateNumberGc())
                || !equalsOrBothNull(oldData.getVehicleType(), newData.getVehicleType())
                || !equalsOrBothNull(oldData.getVehicleContainertype(), newData.getVehicleContainertype())
                || !equalsOrBothNull(oldData.getGoodsType(), newData.getGoodsType())
                || !equalsOrBothNull(oldData.getLoadRate(), newData.getLoadRate())
                || !equalsOrBothNull(oldData.getPasscodeExTime(), newData.getPasscodeExTime());
    }

    /**
     * null-safe 比较
     */
    private boolean equalsOrBothNull(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }


    // ================================================================
    // 【Dashboard 统计接口】
    // ================================================================

    /**
     * 获取 Dashboard 统计数据
     *
     * @param timeType 时间类型：day=今日, month=本月, year=本年
     * @return 统计数据集合
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取首页统计数据", description = "返回统计、预警看板、最近记录等数据，支持日/月/年切换")
    public ApiResponse<Map<String, Object>> getDashboardStats(
            @Parameter(description = "时间类型：day=今日, month=本月, year=本年")
            @RequestParam(defaultValue = "day") String timeType) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime = now.plusDays(1); // 默认查询到明天

        switch (timeType) {
            case "month":
                startTime = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                endTime = now.toLocalDate().plusMonths(1).withDayOfMonth(1).atStartOfDay();
                break;
            case "year":
                startTime = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                endTime = now.toLocalDate().plusYears(1).withDayOfYear(1).atStartOfDay();
                break;
            case "day":
            default:
                startTime = now.toLocalDate().atStartOfDay();
                endTime = startTime.plusDays(1);
                break;
        }

        Map<String, Object> data = new HashMap<>();

        // 信息总览（按时间范围统计）
        data.put("infoOverview", inspectionService.getInfoOverview(startTime, endTime));

        // 查验时段分布（24小时，按指定时间范围）
        data.put("hourlyDistribution", inspectionService.getHourlyDistributionByRange(startTime, endTime));

        // 车型分布（横向条形图）
        data.put("vehicleTypeStats", inspectionService.getVehicleTypeStats(startTime, endTime));

        // 货物类别占比
        data.put("goodsTypeStats", inspectionService.getGoodsTypeStats(startTime, endTime));

        // 待办事项
        data.put("todoItems", inspectionService.getTodoItems());

        // 文件通知（模拟数据）
        data.put("notices", inspectionService.getNotices());

        // 免检比例
        data.put("exemptRate", inspectionService.getExemptRate(startTime, endTime));

        return ApiResponse.success(data);
    }

    /**
     * 获取大屏统计数据
     *
     * @return 关键指标数据（今日通行、总绿通、总通行金额、伪绿通）
     */
    @GetMapping("/datascreen")
    @Operation(summary = "获取大屏关键指标", description = "返回大屏所需的今日通行车辆、总绿通车辆、总通行金额、伪绿通车辆")
    public ApiResponse<Map<String, Object>> getDatascreenStats() {
        Map<String, Object> stats = inspectionService.getDatascreenStats();

        // 转换字段名以匹配前端
        Map<String, Object> result = new HashMap<>();
        result.put("tadaytotal", stats.get("todayTotal"));
        result.put("total", stats.get("total"));
        result.put("discount", stats.get("totalPassAmount"));  // 总通行金额
        result.put("abnormal", stats.get("fakeGreenCount"));  // 伪绿通

        return ApiResponse.success(result);
    }

    /**
     * 获取大屏最近通行记录
     *
     * @return 最近50条查验记录
     */
    @GetMapping("/datascreen/records")
    @Operation(summary = "获取大屏最近通行记录", description = "返回最近50条查验记录用于滚动显示")
    public ApiResponse<List<Map<String, Object>>> getDatascreenRecords() {
        List<Map<String, Object>> records = inspectionService.getRecentRecords(50)
                .stream().map(this::convertToMap).toList();
        return ApiResponse.success(records);
    }

    /**
     * 获取大屏信用记录排行榜
     *
     * @return 合格次数最多的前3辆车（车牌号、合格次数、总次数、10分制信用评分）
     */
    @GetMapping("/datascreen/credit")
    @Operation(summary = "获取大屏信用记录排行榜", description = "返回信用评分最高的前3辆车")
    public ApiResponse<List<Map<String, Object>>> getCreditRanking() {
        List<Map<String, Object>> ranking = inspectionService.getCreditRanking();
        return ApiResponse.success(ranking);
    }

    /**
     * 获取大屏货物类型词云数据
     *
     * @return 货物品种名称和出现次数（联表查询）
     */
    @GetMapping("/datascreen/goods-cloud")
    @Operation(summary = "获取大屏货物类型词云", description = "返回货物品种出现次数用于词云图")
    public ApiResponse<List<Map<String, Object>>> getGoodsTypeCloud() {
        List<Map<String, Object>> goodsCloud = inspectionService.getGoodsTypeStatsForCloud();
        return ApiResponse.success(goodsCloud);
    }

    // ================================================================
    // 【私有辅助方法】
    // ================================================================

    /**
     * 将实体对象转换为前端所需的 Map 结构
     *
     * 【码值转换规则】
     * - resultStatus → resultStatusText（合格/不合格/待查验）
     * - nopassType  → nopassTypeText（不合格具体原因）
     * - vehicleType → vehicleClassText（一型货车~六型货车）
     * - 颜色/货箱类型/介质类型 → 文本
     *
     * @param v 车辆查验实体
     * @return 包含所有字段及文本转换结果的 Map
     */
    private Map<String, Object> convertToMap(VehicleInspection v) {
        Map<String, Object> map = new HashMap<>();

        // 基础信息
        map.put("id", v.getId());
        map.put("plateNumber", v.getPlateNumber());
        map.put("plateNumberGc", v.getPlateNumberGc());
        map.put("driverPhone", v.getDriverPhone());
        map.put("vehicleType", v.getVehicleType());
        map.put("vehicleTypeText", VehicleConstants.getVehicleTypeText(v.getVehicleType()));
        map.put("vehicleContainerType", v.getVehicleContainertype());
        map.put("vehicleContainerTypeText", VehicleConstants.getContainerTypeText(v.getVehicleContainertype()));

        // 货物信息
        map.put("goodsType", v.getGoodsType());
        map.put("goodsCategory", v.getGoodsCategory());
        map.put("loadRate", v.getLoadRate());
        map.put("loadWeight", v.getLoadWeight());
        map.put("vehicleSize", v.getVehicleSize());
        map.put("historyRecord", v.getHistoryRecord());
        // 货物名称：通过 product_code 查 agricultural_products.variety_name，未匹配显示"未知"
        map.put("goodsTypeName", resolveGoodsTypeName(v.getGoodsType()));

        // 图片路径（八个）
        map.put("bodyImagePath", v.getBodyImagePath());
        map.put("transparentImagePath", v.getTransparentImagePath());
        map.put("headImagePath", v.getHeadImagePath());
        map.put("tailImagePath", v.getTailImagePath());
        map.put("topImagePath", v.getTopImagePath());
        map.put("goodsImagePath", v.getGoodsImagePath());
        map.put("evidencesImagePath", v.getEvidencesImagePath());
        map.put("licenseImagePath", v.getLicenseImagePath());
        map.put("passcodeImagePath", v.getPasscodeImagePath());

        // 通行码信息
        map.put("passcodeVehicleId", v.getPasscodeVehicleId());
        map.put("passcodeVehicleDisplayId", v.getPasscodeVehicleDisplayId());
        map.put("passcodeVehicleColorName", v.getPasscodeVehicleColorName());
        map.put("passcodeVehicleColorText", VehicleConstants.getVehicleColorText(v.getPasscodeVehicleColorName()));
        map.put("passcodeEnStationId", v.getPasscodeEnStationId());
        map.put("passcodeExStationId", v.getPasscodeExStationId());
        map.put("passcodeEnWeight", v.getPasscodeEnWeight());
        map.put("passcodeExWeight", v.getPasscodeExWeight());
        map.put("passcodeMediaType", v.getPasscodeMediaType());
        map.put("passcodeMediaTypeText", VehicleConstants.getMediaTypeText(v.getPasscodeMediaType()));
        map.put("passcodeTransactionId", v.getPasscodeTransactionId());
        map.put("passcodePassId", v.getPasscodePassId());
        map.put("passcodeExTime", v.getPasscodeExTime());
        map.put("passcodeTransPayType", v.getPasscodeTransPayType());
        map.put("passcodeTransPayTypeText", VehicleConstants.getTransPayTypeText(v.getPasscodeTransPayType()));
        map.put("passcodeFee", v.getPasscodeFee());
        map.put("passcodePayFee", v.getPasscodePayFee());
        map.put("passcodeVehicleSign", v.getPasscodeVehicleSign());
        map.put("passcodeVehicleSignText", VehicleConstants.getVehicleSignText(v.getPasscodeVehicleSign()));
        map.put("passcodeProvinceCount", v.getPasscodeProvinceCount());

        // 查验业务信息
        map.put("operatorName", v.getOperatorName());
        map.put("acceptanceTime", v.getAcceptanceTime());
        map.put("inspectionTime", v.getInspectionTime());
        map.put("createdTime", v.getCreatedTime());
        map.put("updatedTime", v.getUpdatedTime());
        map.put("resultStatus", v.getResultStatus());
        map.put("resultStatusText", VehicleConstants.getResultStatusText(v.getResultStatus()));
        map.put("nopassType", v.getNopassType());
        map.put("nopassTypeText", VehicleConstants.getNopassTypeText(v.getNopassType()));
        map.put("status", v.getStatus());
        map.put("statusText", VehicleConstants.getStatusText(v.getStatus()));
        map.put("groupId", v.getGroupId());
        map.put("groupName", resolveGroupName(v.getGroupId()));
        map.put("inspectorPhone", v.getInspectorPhone());
        map.put("reviewerPhone", v.getReviewerPhone());
        map.put("manualReviewState", v.getManualReviewState());
        map.put("manualReviewText", VehicleConstants.getManualReviewText(v.getManualReviewState()));
        map.put("toTransportdeptState", v.getToTransportdeptState());
        map.put("toTransportdeptStateText", VehicleConstants.getTransportDeptStateText(v.getToTransportdeptState()));
        map.put("toTransportdeptTime", v.getToTransportdeptTime());
        map.put("toTransportdeptComment", v.getToTransportdeptComment());
        // 查验依据：暂无对应字段，直接显示"等待开发"
        map.put("inspectionBasis", "等待开发");

        return map;
    }

    /**
     * 根据 goods_type（product_code）查询农产品品种名称
     *
     * 【查询逻辑】
     * goods_type 存储的是 agricultural_products.product_code，
     * 通过 product_code 匹配查 variety_name，未匹配到则返回"未知"。
     *
     * @param goodsType 产品编码（product_code）
     * @return 品种名称，未匹配返回"未知"
     */
    private String resolveGoodsTypeName(String goodsType) {
        if (goodsType == null || goodsType.isEmpty()) return "-";
        // 支持多编码格式（如 "10601|10801"），按 | 分割逐个匹配
        String[] codes = goodsType.split("\\|");
        if (codes.length == 1) {
            // 单编码：直接精确匹配
            AgriculturalProduct product = agriculturalProductMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgriculturalProduct>()
                            .eq(AgriculturalProduct::getProductCode, goodsType.trim())
            );
            return product != null ? product.getVarietyName() : "未知";
        } else {
            // 多编码：拼接多个品种名称
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < codes.length; i++) {
                String code = codes[i].trim();
                if (code.isEmpty()) continue;
                AgriculturalProduct product = agriculturalProductMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgriculturalProduct>()
                                .eq(AgriculturalProduct::getProductCode, code)
                );
                if (product != null) {
                    if (sb.length() > 0) sb.append("|");
                    sb.append(product.getVarietyName());
                }
            }
            return sb.length() > 0 ? sb.toString() : "未知";
        }
    }

    /**
     * 根据 groupId 解析班组名称
     * 班组表已删除，直接返回"班组X"格式
     */
    private String resolveGroupName(String groupId) {
        if (groupId == null || groupId.isBlank()) return null;
        return "班组" + groupId;
    }
}
