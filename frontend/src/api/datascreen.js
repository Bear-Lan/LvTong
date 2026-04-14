/**
 * 大屏模块 API 接口封装
 *
 * 【模块说明】
 * 统一封装所有大屏相关的 HTTP 请求，
 * 包括关键指标、通行记录、信用排行、货物词云等数据接口。
 */

import request from '@/utils/request'

/**
 * 获取大屏关键指标数据
 * @returns 今日通行车辆、总绿通车辆、总通行金额、伪绿通车辆
 */
export function getKpiData() {
    return request.get('/inspection/datascreen')
}

/**
 * 获取大屏最近通行记录
 * @param {number} limit - 返回记录数量，默认50
 * @returns 最近查验记录列表
 */
export function getPassRecords(limit = 50) {
    return request.get(`/inspection/datascreen/records`)
}

/**
 * 获取大屏信用记录排行
 * @returns 合格次数最多的前3辆车（车牌号、合格次数、总次数、10分制信用评分）
 */
export function getCreditRanking() {
    return request.get('/inspection/datascreen/credit')
}

/**
 * 获取大屏货物类型词云数据
 * @returns 货物品种名称和出现次数
 */
export function getGoodsTypeCloud() {
    return request.get('/inspection/datascreen/goods-cloud')
}


