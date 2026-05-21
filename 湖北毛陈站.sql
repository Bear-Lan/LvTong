/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : three_platform

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 19/05/2026 18:21:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for 湖北毛陈站
-- ----------------------------
DROP TABLE IF EXISTS `湖北毛陈站`;
CREATE TABLE `湖北毛陈站`  (
  `check_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '查验人_司机手机号',
  `id` int NOT NULL AUTO_INCREMENT,
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车牌号',
  `plate_number_gc` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '挂车号码',
  `driver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '司机电话',
  `vehicle_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车型（货车/客车）',
  `vehicle_containertype` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车厢类型',
  `goods_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物类型',
  `goods_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车厢分类（厢式/罐式）',
  `load_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '满载率',
  `load_weight` decimal(8, 2) NULL DEFAULT NULL COMMENT '载重率',
  `vehicle_size` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '长宽高',
  `history_record` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '历史记录',
  `body_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车身影像路径',
  `transparent_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '透视影像路径',
  `head_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车头照片路径',
  `tail_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车尾照片路径',
  `top_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车顶照片路径',
  `goods_image_path` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物照片路径',
  `evidences_image_path` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '货物照片路径',
  `license_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '行驶证照片路径',
  `passcode_image_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行证照片路径',
  `passcode_vehicle_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车辆ID',
  `passcode_vehicle_display_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车辆显示ID',
  `passcode_vehicle_color_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行码车牌颜色',
  `passcode_en_station_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入口站ID',
  `passcode_ex_station_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口站ID',
  `passcode_en_weight` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '入口重量',
  `passcode_ex_weight` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口重量',
  `passcode_media_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '介质类型',
  `passcode_transaction_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易ID',
  `passcode_pass_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通行证ID',
  `passcode_ex_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出口时间',
  `passcode_trans_pay_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易支付类型',
  `passcode_fee` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '费用',
  `passcode_pay_fee` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '支付费用',
  `passcode_vehicle_sign` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车辆标志',
  `passcode_province_count` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '省份数量',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作员姓名',
  `btn_prebook_time` datetime NULL DEFAULT NULL COMMENT '司机按键预约',
  `acceptance_time` datetime NULL DEFAULT NULL COMMENT '受理时间',
  `opengate_time` datetime NULL DEFAULT NULL COMMENT '抬杆放行时间',
  `openlightscreen_time` datetime NULL DEFAULT NULL COMMENT '光慕打开时间',
  `closelightscreen_time` datetime NULL DEFAULT NULL COMMENT '光慕关闭时间',
  `cd_photo_time` datetime NULL DEFAULT NULL COMMENT '检测时间',
  `inspection_time` datetime NULL DEFAULT NULL COMMENT '检测时间',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `result_status` int NULL DEFAULT NULL COMMENT '查验结果',
  `nopass_type` int NULL DEFAULT NULL COMMENT '不合格类型',
  `status` int NULL DEFAULT NULL COMMENT '状态',
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班组编号',
  `inspector_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验货人用户手机号',
  `reviewer_phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '复核人用户手机号',
  `manual_review_state` int NULL DEFAULT 0 COMMENT '人工审核  0：未审核 1：审核',
  `to_transportdept_state` int NULL DEFAULT 0 COMMENT '上传状态',
  `to_transportdept_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `to_transportdept_comment` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '上传结果',
  PRIMARY KEY (`id` DESC) USING BTREE,
  INDEX `idx_plate_number`(`plate_number` ASC) USING BTREE,
  INDEX `idx_inspection_time`(`inspection_time` ASC) USING BTREE,
  INDEX `idx_operator_name`(`operator_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1216 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '车辆检测记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
