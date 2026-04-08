<template>
  <!--
    查验详情弹窗
    左右两栏布局：
      - 左侧栏（约 2/3）：证据链照片、查验数据、查验结论
      - 右侧栏（约 1/3）：货物照片（可滚动）
  -->
  <el-dialog
    v-model="visible"
    :title="editable ? '编辑查验记录' : '查验详情'"
    width="95vw"
    top="0vh"
    destroy-on-close
    class="inspection-detail-dialog"
  >

    <!-- ================== 左右两栏整体布局 ================== -->
    <div class="detail-body">

      <!-- ================== 左侧栏（约 2/3） ================== -->
      <div class="detail-left">

        <!-- 第一区域：证据链照片
             第一行：车头照、车尾照、行驶证、顶部照、通行凭证（5列）
             第二行：透视影像、车身照（2列，重点图片） -->
        <div class="detail-section evidence-section">
          <div class="section-header">
            <span class="section-title">证据链照片</span>
            <span class="section-sub">点击图片可放大查看</span>
          </div>
          <!-- 第一行：5列等宽网格 -->
          <div class="evidence-grid">

            <!-- 车头照（type-tag=11） -->
            <div class="evidence-item" :class="{ 'no-image': !row.headImagePath }">
              <div class="evidence-img-box" v-if="row.headImagePath">
                <el-image
                  :src="formatImageUrl(row.headImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.headImagePath)]"
                  class="evidence-img"
                  :initial-index="0"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">11</span>车头照
              </div>
            </div>

            <!-- 车尾照（type-tag=12） -->
            <div class="evidence-item" :class="{ 'no-image': !row.tailImagePath }">
              <div class="evidence-img-box" v-if="row.tailImagePath">
                <el-image
                  :src="formatImageUrl(row.tailImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.tailImagePath)]"
                  class="evidence-img"
                  :initial-index="1"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">12</span>车尾照
              </div>
            </div>

            <!-- 行驶证（type-tag=13） -->
            <div class="evidence-item" :class="{ 'no-image': !row.licenseImagePath }">
              <div class="evidence-img-box" v-if="row.licenseImagePath">
                <el-image
                  :src="formatImageUrl(row.licenseImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.licenseImagePath)]"
                  class="evidence-img"
                  :initial-index="2"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">13</span>行驶证
              </div>
            </div>

            <!-- 顶部照 -->
            <div class="evidence-item" :class="{ 'no-image': !row.topImagePath }">
              <div class="evidence-img-box" v-if="row.topImagePath">
                <el-image
                  :src="formatImageUrl(row.topImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.topImagePath)]"
                  class="evidence-img"
                  :initial-index="3"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">26</span>顶部照
              </div>
            </div>

            <!-- 通行凭证 -->
            <div class="evidence-item" :class="{ 'no-image': !row.passcodeImagePath }">
              <div class="evidence-img-box" v-if="row.passcodeImagePath">
                <el-image
                  :src="formatImageUrl(row.passcodeImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.passcodeImagePath)]"
                  class="evidence-img"
                  :initial-index="4"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
              <div class="evidence-label">
                <span class="type-tag">凭证</span>通行凭证
              </div>
            </div>
          </div>

          <!-- 第二行：透视影像 + 车身照（重要图片，2列各占50%） -->
          <div class="evidence-grid evidence-grid-2col">
            <!-- 透视影像（ETC X光扫描） -->
            <div class="evidence-item evidence-item-horizontal" :class="{ 'no-image': !row.transparentImagePath }">
              <div class="evidence-label-left">
                <span class="type-tag">X光</span>透视影像
              </div>
              <div class="evidence-img-box" v-if="row.transparentImagePath">
                <el-image
                  :src="formatImageUrl(row.transparentImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.transparentImagePath)]"
                  class="evidence-img"
                  :initial-index="5"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
            </div>

            <!-- 车身照（type-tag=25） -->
            <div class="evidence-item evidence-item-horizontal" :class="{ 'no-image': !row.bodyImagePath }">
              <div class="evidence-label-left">
                <span class="type-tag">25</span>车身照
              </div>
              <div class="evidence-img-box" v-if="row.bodyImagePath">
                <el-image
                  :src="formatImageUrl(row.bodyImagePath)"
                  fit="contain"
                  :preview-src-list="[formatImageUrl(row.bodyImagePath)]"
                  class="evidence-img"
                  :initial-index="6"
                />
              </div>
              <div class="evidence-placeholder" v-else>
                <el-icon><Picture /></el-icon>
              </div>
            </div>
          </div>
        </div>

      <!-- 第二区域：数据网格（4列） -->
      <div class="detail-section data-section">
        <div class="section-header">
          <span class="section-title">查验数据</span>
        </div>
        <div class="data-grid">

          <!-- 第一列：基础通行信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">入口站名称</span>
              <span class="data-value">{{ row.passcodeEnStationId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口站名称</span>
              <span class="data-value">{{ row.passcodeExStationId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口交易时间</span>
              <span class="data-value">{{ row.passcodeExTime || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">总交易金额(元)</span>
              <span class="data-value">{{ row.passcodeFee || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口交易编号</span>
              <span class="data-value mono">{{ row.passcodeTransactionId || '-' }}</span>
            </div>
            <!-- 备注内容：可编辑时显示输入框 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">备注内容</span>
              <el-input v-model="form.historyRecord" placeholder="历史查验记录备注" size="small" style="width: 100%;" />
            </div>
            <div class="data-row" v-else>
              <span class="data-label">备注内容</span>
              <span class="data-value">{{ row.historyRecord || '-' }}</span>
            </div>
          </div>

          <!-- 第二列：车辆与货物信息 -->
          <div class="data-col">
            <!-- 货车类型：可编辑时显示下拉 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">货车类型</span>
              <el-select v-model="form.vehicleType" placeholder="请选择" clearable size="small" style="width: 100%;">
                <el-option label="一型货车" value="11" />
                <el-option label="二型货车" value="12" />
                <el-option label="三型货车" value="13" />
                <el-option label="四型货车" value="14" />
                <el-option label="五型货车" value="15" />
                <el-option label="六型货车" value="16" />
              </el-select>
            </div>
            <div class="data-row" v-else>
              <span class="data-label">货车类型</span>
              <span class="data-value primary">{{ row.vehicleTypeText || '-' }}</span>
            </div>
            <!-- 货箱类型：可编辑时显示下拉 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">货箱类型</span>
              <el-select v-model="form.vehicleContainertype" placeholder="请选择" clearable size="small" style="width: 100%;">
                <el-option label="罐式货车" value="1" />
                <el-option label="敞篷货车（平板式）" value="2.1" />
                <el-option label="敞篷货车（栅栏式）" value="2.2" />
                <el-option label="普通货车(篷布包裹式)" value="3.1" />
                <el-option label="厢式货车(封闭货车)" value="4.1" />
                <el-option label="特殊结构货车(水箱式)" value="5.1" />
              </el-select>
            </div>
            <div class="data-row" v-else>
              <span class="data-label">货箱类型</span>
              <span class="data-value">{{ row.vehicleContainerTypeText || '-' }}</span>
            </div>
            <!-- 满载率：可编辑时显示输入框 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">满载率(%)</span>
              <el-input-number v-model="form.loadRate" :min="0" :max="100" :precision="2" size="small" style="width: 100%;" placeholder="0-100" />
            </div>
            <div class="data-row" v-else>
              <span class="data-label">满载率(%)</span>
              <span class="data-value">{{ row.loadRate != null ? row.loadRate + '%' : '-' }}</span>
            </div>
            <!-- 货物名称：可编辑时显示选择品种 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">货物名称</span>
              <div class="goods-type-field">
                <div class="selected-tags" v-if="selectedProducts.length > 0">
                  <el-tag
                    v-for="code in selectedProducts"
                    :key="code"
                    closable
                    @close="removeProduct(code)"
                  >{{ getVarietyName(code) }}</el-tag>
                </div>
                <el-button size="small" @click="openGoodsDialog">
                  <el-icon><Plus /></el-icon> 选择品种
                </el-button>
              </div>
            </div>
            <div class="data-row" v-else>
              <span class="data-label">货物名称</span>
              <span class="data-value">{{ row.goodsTypeName || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">入口重量(KG)</span>
              <span class="data-value">{{ row.passcodeEnWeight || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">出口重量(KG)</span>
              <span class="data-value">{{ row.passcodeExWeight || '-' }}</span>
            </div>
          </div>

          <!-- 第三列：载重与费用信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">应收金额(元)</span>
              <span class="data-value">{{ row.passcodePayFee || '-' }}</span>
            </div>
            <!-- 货车长宽高：可编辑时显示输入框 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">货车长宽高</span>
              <el-input v-model="form.vehicleSize" placeholder="如：18000×2500×4000" size="small" style="width: 100%;" />
            </div>
            <div class="data-row" v-else>
              <span class="data-label">货车长宽高</span>
              <span class="data-value mono">{{ formatVehicleSize(row.vehicleSize) }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通过省份个数</span>
              <span class="data-value">{{ row.passcodeProvinceCount || '-' }}</span>
            </div>
            <!-- 司机电话：可编辑时显示输入框 -->
            <div class="data-row" v-if="editable">
              <span class="data-label">司机电话</span>
              <el-input v-model="form.driverPhone" placeholder="请输入手机号" size="small" style="width: 100%;" />
            </div>
            <div class="data-row" v-else>
              <span class="data-label">司机电话</span>
              <span class="data-value">{{ row.driverPhone || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通行标识ID</span>
              <span class="data-value mono">{{ row.passcodePassId || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">通行介质</span>
              <span class="data-value">{{ row.passcodeMediaTypeText || '-' }}</span>
            </div>
          </div>

          <!-- 第四列：车牌与状态信息 -->
          <div class="data-col">
            <div class="data-row">
              <span class="data-label">车牌号码</span>
              <span class="data-value plate">{{ row.plateNumber || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">挂车号码</span>
              <span class="data-value">{{ row.plateNumberGc || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">交易支付方式</span>
              <span class="data-value">{{ row.passcodeTransPayTypeText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">车辆状态标识</span>
              <span class="data-value">{{ row.passcodeVehicleSignText || '-' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">查验依据</span>
              <span class="data-value">{{ row.inspectionBasis || '等待开发' }}</span>
            </div>
            <div class="data-row">
              <span class="data-label">复核结果</span>
              <el-select v-if="editable" v-model="form.manualReviewState" placeholder="请选择" size="small" style="width: 140px;">
                <el-option label="未审核" :value="0" />
                <el-option label="已审核" :value="1" />
                <el-option label="审核未通过" :value="2" />
              </el-select>
              <span v-else class="data-value">
                {{ row.manualReviewText || '未审核' }}
              </span>
            </div>
          </div>

        </div>
      </div>

      <!-- 底部结论栏 -->
      <div class="result-bar">
        <div class="result-bar-main">
          <!-- 查验结果：可编辑时显示下拉 -->
          <template v-if="editable">
            <el-select v-model="form.resultStatus" size="large" style="width: 120px;">
              <el-option label="待查验" :value="0" />
              <el-option label="合格" :value="1" />
              <el-option label="不合格" :value="2" />
            </el-select>
          </template>
          <template v-else>
            <el-tag :type="getResultTagType(row.resultStatus)" size="large" effect="dark" class="result-badge">
              {{ row.resultStatusText || '-' }}
            </el-tag>
          </template>
        </div>
        <div class="result-bar-items">
          <!-- 验货员：显示 inspector_phone，不可编辑 -->
          <div class="result-item">
            <span class="result-label">验货员</span>
            <span class="result-value">{{ row.inspectorPhone || '-' }}</span>
          </div>
          <!-- 复核员：可编辑时显示下拉 -->
          <div class="result-item" v-if="editable">
            <span class="result-label">复核员</span>
            <el-select v-model="form.reviewerPhone" placeholder="请选择核验员" clearable filterable size="small" style="width: 150px;" @change="handleReviewerChange">
              <el-option
                v-for="r in reviewers"
                :key="r.phone"
                :label="r.realName + ' ' + r.phone"
                :value="r.phone"
              />
            </el-select>
          </div>
          <div class="result-item" v-else>
            <span class="result-label">复核员</span>
            <span class="result-value">{{ row.reviewerPhone || '-' }}</span>
          </div>
          <!-- 班组：自动跟随复核员，不可编辑 -->
          <div class="result-item">
            <span class="result-label">班组</span>
            <span class="result-value">{{ form.groupId || row.groupId || '-' }}</span>
          </div>
          <!-- 不合格类型：可编辑时显示下拉选择 -->
          <div class="result-item" v-if="editable">
            <span class="result-label">不合格类型</span>
            <el-select v-model="form.nopassType" placeholder="合格则不填" clearable size="small" style="width: 200px;">
              <el-option
                v-for="opt in nopassTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
          <div class="result-item" v-else-if="row.resultStatus === 2">
            <span class="result-label">不合格类型</span>
            <span class="result-value danger">{{ row.nopassTypeText || row.nopassType || '-' }}</span>
          </div>
        </div>
      </div>

      </div><!-- /detail-left -->

      <!-- ================== 右侧栏（约 1/3）：货物照片 ================== -->
      <div class="detail-right">
        <div class="detail-section goods-section">
          <div class="section-header">
            <span class="section-title">货物照</span>
            <span class="section-sub" v-if="goodsImages.length > 0">共 {{ goodsImages.length }} 张</span>
          </div>
          <div class="goods-photo-area">
            <div v-if="goodsImages.length > 0" class="goods-photo-list">
              <div
                v-for="(img, idx) in goodsImages"
                :key="idx"
                class="goods-photo-item"
              >
                <el-image
                  :src="formatImageUrl(img)"
                  fit="contain"
                  class="goods-photo-img"
                  :preview-src-list="goodsImages.map(p => formatImageUrl(p))"
                  :initial-index="idx"
                />
                <div class="goods-photo-label">货物照 {{ idx + 1 }}</div>
              </div>
            </div>
            <div class="goods-photo-placeholder" v-else>
              <el-icon size="40" color="#dcdfe6"><Picture /></el-icon>
              <span>暂无货物照</span>
            </div>
          </div>
        </div>
      </div><!-- /detail-right -->

    </div><!-- /detail-body -->

    <!-- 货物类型选择弹窗 -->
    <el-dialog
      v-model="goodsDialogVisible"
      title="选择货物类型"
      width="1000px"
      destroy-on-close
      class="goods-dialog"
      append-to-body
    >
      <div class="goods-dialog-toolbar">
        <el-select
          v-model="filterProductType"
          placeholder="按产品大类筛选"
          clearable
          size="default"
          style="width: 180px;"
        >
          <el-option
            v-for="pt in productTypeOptions"
            :key="pt"
            :label="pt"
            :value="pt"
          />
        </el-select>
        <el-select
          v-model="filterCategory"
          placeholder="按类别筛选"
          clearable
          size="default"
          style="width: 180px;"
          :disabled="!filterProductType"
        >
          <el-option
            v-for="cat in categoryOptions"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
        <el-input
          v-model="filterVarietyName"
          placeholder="搜索品种名称"
          clearable
          size="default"
          style="flex: 1; min-width: 160px;"
        />
      </div>

      <div v-if="tempSelected.length > 0" class="temp-selected">
        <span class="temp-label">已选：</span>
        <el-tag
          v-for="code in tempSelected"
          :key="code"
          closable
          type="success"
          size="small"
          @close="removeTemp(code)"
        >{{ getVarietyName(code) }}</el-tag>
      </div>

      <div class="variety-card-grid">
        <div
          v-for="v in displayedVarieties"
          :key="v.productCode"
          class="variety-card"
          :class="{ selected: tempSelected.includes(v.productCode) }"
          @click="toggleTemp(v.productCode)"
        >
          <div class="card-left">
            <div class="card-type">{{ v.productType }}</div>
            <div class="card-name">{{ v.varietyName }}</div>
            <div v-if="getAliasesText(v.aliases)" class="card-aliases">{{ getAliasesText(v.aliases) }}</div>
          </div>
          <div class="card-icon-wrapper">
            <img
              v-if="getVarietyImage(v.varietyName)"
              :src="getVarietyImage(v.varietyName)"
              :alt="v.varietyName"
              class="variety-img"
            />
            <el-icon v-else size="20" color="#c0c4cc"><Picture /></el-icon>
          </div>
        </div>
        <div v-if="displayedVarieties.length === 0" class="no-data">
          暂无匹配的品种
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer-inner">
          <span class="selected-count">已选 {{ tempSelected.length }} 个品种</span>
          <div style="display: flex; gap: 10px;">
            <el-button @click="goodsDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="confirmGoodsDialog">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 底部按钮：编辑模式下显示保存按钮 -->
    <template #footer v-if="editable">
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</el-button>
      </div>
    </template>

  </el-dialog>
</template>

<script setup>
/**
 * InspectionDetail 查验详情弹窗组件
 *
 * 【数据来源】
 * 父组件（HistoricalRecords.vue）通过 props.row 传入当前选中行的完整数据。
 * row 来自后端接口返回，数据已在 Controller.convertToMap() 中完成码值到文本的转换。
 *
 * 【不需要额外请求】
 * 详情页不需要再次调用 API，直接使用父组件传入的数据。
 * 因为 HistoricalRecords 列表页已经返回了所有字段的完整数据。
 *
 * 【v-model 说明】
 * visible 控制弹窗显示/隐藏，通过 emit('update:modelValue') 通知父组件。
 * destroy-on-close 确保关闭弹窗时销毁 DOM，重开时状态干净。
 */

import { computed, ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'
import { getProductList, getNopassTypeOptions, updateInspection } from '@/api/vehicleInspection'
import { getUserPhoneList } from '@/api/user'

// ================================================================
// Props & Emits
// ================================================================

/**
 * modelValue：控制弹窗显示（v-model）
 * row：当前选中行的完整数据对象（从 HistoricalRecords 传入）
 * editable：是否为编辑模式（默认 false，展示模式）
 */
const props = defineProps({
  modelValue: Boolean,
  row: { type: Object, default: () => ({}) },
  editable: { type: Boolean, default: false }
})

/**
 * update:modelValue：关闭时通知父组件更新 v-model
 * refresh：删除/编辑成功后通知父组件刷新（当前详情页未直接使用）
 */
const emit = defineEmits(['update:modelValue', 'refresh'])

// ================================================================
// 弹窗显示状态（computed 实现 v-model 双向绑定）
// ================================================================

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// ================================================================
// 样式辅助函数
// ================================================================

/**
 * getResultTagType：根据查验结果返回 el-tag 类型
 */
const getResultTagType = (status) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

// ================================================================
// 图片路径转换
// ================================================================

/**
 * formatImageUrl：将本地路径转换为后端图片接口 URL
 *
 * 【背景】
 * 数据库中图片路径以 D:\xxx\xxx.jpg 格式存储（Windows 本地路径）。
 * 浏览器无法直接通过 file:// 协议访问，需通过后端 /api/images 接口转发。
 *
 * 【处理逻辑】
 * 1. 空路径直接返回空字符串（el-image 收到空字符串不显示）
 * 2. 对路径进行 URL 编码后拼接到 /api/images?path=
 * 3. axios 请求会自动附上 JWT Token，接口鉴权通过
 *
 * @param {string|null|undefined} path 原始文件路径
 * @returns {string} 后端图片接口 URL
 *
 * @example
 * formatImageUrl('D:\\photos\\car.jpg')  → '/api/images?path=D%3A%5Cphotos%5Ccar.jpg'
 * formatImageUrl(null)                   → ''
 */
const formatImageUrl = (path) => {
  if (!path) return ''
  return `/api/images?path=${encodeURIComponent(path)}`
}

/** 货物照数组：支持中文逗号、英文逗号分隔多张图片路径 */
const goodsImages = computed(() => {
  if (!props.row.goodsImagePath) return []
  return props.row.goodsImagePath
    .split(/[,，]/)
    .map(p => p.trim())
    .filter(p => p && (p.startsWith('/') || /^[A-Za-z]:/.test(p)))
})

/**
 * formatVehicleSize：格式化货车长宽高
 * 数据库中格式为"长|宽|高"（单位mm），如 "8870|2260|3730"
 * 转换为 "长:8.87m × 宽:2.26m × 高:3.73m"
 */
const formatVehicleSize = (size) => {
  if (!size) return '-'
  const parts = size.split('|')
  if (parts.length === 3) {
    return `长:${(parseFloat(parts[0]) / 1000).toFixed(2)}m × 宽:${(parseFloat(parts[1]) / 1000).toFixed(2)}m × 高:${(parseFloat(parts[2]) / 1000).toFixed(2)}m`
  }
  return size
}

// ================================================================
// 编辑模式相关
// ================================================================

/** 货物类型选中值（数组） */
const selectedProducts = ref([])
const goodsDialogVisible = ref(false)
const tempSelected = ref([])
const filterProductType = ref('')
const filterCategory = ref('')
const filterVarietyName = ref('')

/** 不合格类型选项 */
const nopassTypeOptions = ref([])

/** 核验员列表 */
const reviewers = ref([])

/** 提交按钮 loading */
const submitting = ref(false)

/** 表单数据 */
const form = reactive({
  // 货物信息
  goodsType: '',
  loadRate: null,
  vehicleSize: '',
  // 查验信息
  resultStatus: null,
  nopassType: null,
  operatorName: '',
  reviewerPhone: '',
  manualReviewState: null,
  groupId: '',
  // 备注
  historyRecord: '',
  // 车辆信息
  vehicleType: '',
  vehicleContainertype: '',
  driverPhone: ''
})

/** 品种数据缓存 */
let allVarieties = []
const productTypeOptions = ref([])
const allCategories = ref([])

// 品种图片
const varietyImages = import.meta.glob('@/assets/variety_img/*.png', { eager: true })

const getVarietyImage = (varietyName) => {
  if (!varietyName) return null
  let imagePath = varietyImages[`/src/assets/variety_img/${varietyName}.png`]?.default
  if (imagePath) return imagePath
  for (const path in varietyImages) {
    const fileName = path.split('/').pop().replace('.png', '')
    if (fileName.includes(varietyName) || varietyName.includes(fileName)) {
      return varietyImages[path].default
    }
  }
  return null
}

const categoryOptions = computed(() => {
  if (!filterProductType.value) return allCategories.value
  return [...new Set(
    allVarieties
      .filter(v => v.productType === filterProductType.value)
      .map(v => v.category)
      .filter(c => c)
  )].sort()
})

const displayedVarieties = computed(() => {
  let list = allVarieties
  if (filterProductType.value) {
    list = list.filter(v => v.productType === filterProductType.value)
  }
  if (filterCategory.value) {
    list = list.filter(v => v.category === filterCategory.value)
  }
  if (filterVarietyName.value.trim()) {
    const kw = filterVarietyName.value.trim().toLowerCase()
    list = list.filter(v => v.varietyName.toLowerCase().includes(kw))
  }
  return list
})

const getVarietyName = (code) => {
  const v = allVarieties.find(v => v.productCode === code)
  return v ? v.varietyName : code
}

const getAliasesText = (aliasesJson) => {
  if (!aliasesJson) return ''
  try {
    const arr = JSON.parse(aliasesJson)
    return Array.isArray(arr) ? arr.join('、') : ''
  } catch {
    return ''
  }
}

const loadProducts = async () => {
  try {
    const res = await getProductList()
    if (res.code === 200) {
      allVarieties = res.data.varieties || []
      productTypeOptions.value = res.data.productTypes || []
      allCategories.value = res.data.categories || []
    }
  } catch {
    ElMessage.error('货物类型加载失败')
  }
}

const loadNopassTypes = async () => {
  try {
    const res = await getNopassTypeOptions()
    if (res.code === 200) {
      nopassTypeOptions.value = res.data || []
    }
  } catch {
    ElMessage.error('不合格类型加载失败')
  }
}

const loadReviewers = async () => {
  try {
    const res = await getUserPhoneList()
    if (res.code === 200) {
      reviewers.value = res.data || []
    }
  } catch {
    // 核验员加载失败不影响查验记录操作
  }
}

/** 复核员切换时自动更新班组 */
const handleReviewerChange = (phone) => {
  if (!phone) {
    form.groupId = ''
    return
  }
  // 遍历查找匹配的复核员
  for (const r of reviewers.value) {
    if (r.phone === phone) {
      form.groupId = r.groupId ? String(r.groupId) : ''
      break
    }
  }
}

const openGoodsDialog = () => {
  tempSelected.value = [...selectedProducts.value]
  filterProductType.value = ''
  filterCategory.value = ''
  filterVarietyName.value = ''
  goodsDialogVisible.value = true
}

const toggleTemp = (code) => {
  const idx = tempSelected.value.indexOf(code)
  if (idx >= 0) {
    tempSelected.value.splice(idx, 1)
  } else {
    tempSelected.value.push(code)
  }
}

const removeTemp = (code) => {
  tempSelected.value = tempSelected.value.filter(c => c !== code)
}

const confirmGoodsDialog = () => {
  selectedProducts.value = [...tempSelected.value]
  goodsDialogVisible.value = false
}

const removeProduct = (code) => {
  selectedProducts.value = selectedProducts.value.filter(c => c !== code)
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const data = {}
    // 货物类型
    if (selectedProducts.value && selectedProducts.value.length > 0) {
      data.goodsType = [...selectedProducts.value].join('|')
    } else {
      data.goodsType = ''
    }
    // 货物信息
    data.loadRate = form.loadRate
    data.vehicleSize = form.vehicleSize
    // 查验信息
    data.resultStatus = form.resultStatus
    data.nopassType = form.nopassType
    data.operatorName = form.operatorName
    data.reviewerPhone = form.reviewerPhone
    data.manualReviewState = form.manualReviewState
    data.groupId = form.groupId
    data.historyRecord = form.historyRecord
    // 车辆信息
    data.vehicleType = form.vehicleType
    data.vehicleContainertype = form.vehicleContainertype
    data.driverPhone = form.driverPhone

    const res = await updateInspection(props.row.id, data)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      visible.value = false
      emit('refresh')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 监听 row 变化，同步数据到表单
watch(() => props.row, (row) => {
  if (props.editable && row && row.id) {
    // 货物类型
    selectedProducts.value = row.goodsType
      ? row.goodsType.split('|').map(c => c.trim()).filter(c => c)
      : []
    // 货物信息
    form.loadRate = typeof row.loadRate === 'string' ? (row.loadRate === '' ? null : Number(row.loadRate)) : row.loadRate
    form.vehicleSize = row.vehicleSize || ''
    // 查验信息
    form.resultStatus = row.resultStatus
    form.nopassType = row.nopassType
    form.operatorName = row.operatorName || ''
    form.reviewerPhone = row.reviewerPhone || ''
    form.manualReviewState = row.manualReviewState
    form.groupId = row.groupId || ''
    form.historyRecord = row.historyRecord || ''
    // 车辆信息
    form.vehicleType = row.vehicleType || ''
    form.vehicleContainertype = row.vehicleContainerType || ''
    form.driverPhone = row.driverPhone || ''
  }
}, { immediate: true })

onMounted(() => {
  loadProducts()
  loadNopassTypes()
  loadReviewers()
})
</script>

<style scoped>
/* ========== 弹窗样式 ========== */
.inspection-detail-dialog :deep(.el-dialog) {
  max-height: 90vh; /* 内容过多时可滚动 */
}

.inspection-detail-dialog :deep(.el-dialog__body) {
  padding: 0 24px 20px;
}

/* ========== 区域通用样式 ========== */
.detail-section {
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

/* 区域标题栏 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}

.section-sub {
  font-size: 12px;
  color: #909399;
}

/* ========== 左右两栏布局 ========== */

/* 整体两栏：左侧内容 + 右侧货物照 */
.detail-body {
  display: flex;
  align-items: stretch; /* 恢复拉伸对齐，让左右两栏高度一致 */
  gap: 0;
}

/* 左侧栏：约 80%，包含证据链/数据/结论 */
.detail-left {
  flex: 1;
  min-width: 0;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* 右侧栏：约 20%，专用于货物照 */
.detail-right {
  flex: 0 0 18%;
  min-width: 0;
  max-width: 18%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* ========== 证据链照片区域（6列） ========== */

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0;
}

/* 第二行：透视影像+车身照，2列各占50% */
.evidence-grid-2col {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-top: 1px solid #ebeef5;
}

/* 第二行横向布局：标签在左，图片在右 */
.evidence-item-horizontal {
  display: flex;
  align-items: center;
  border-right: 1px solid #f0f0f0;
  padding: 10px 12px;
  gap: 12px;
}

.evidence-item-horizontal:last-child {
  border-right: none;
}

.evidence-item-horizontal .evidence-label-left {
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.evidence-item-horizontal .evidence-img-box {
  flex: 1;
  height: 80px;
  min-width: 0;
}

.evidence-item-horizontal .evidence-placeholder {
  flex: 1;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #dcdfe6;
}

/* 每个照片格子：图片+标签+分隔线 */
.evidence-item {
  border-right: 1px solid #f0f0f0;
  text-align: center;
}

.evidence-item:last-child {
  border-right: none; /* 最后一列去掉右边框 */
}

/* 图片容器：固定高度，灰色背景，鼠标变为放大镜 */
.evidence-img-box {
  height: 80px;
  overflow: hidden;
  background: #f5f7fa;
  cursor: zoom-in;
}

.evidence-img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 保持比例，完整显示图片 */
}

/* 无图片时的占位图标 */
.evidence-placeholder {
  height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #dcdfe6;
}

/* 图片下方标签 */
.evidence-label {
  padding: 10px;
  font-size: 13px;
  color: #606266;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  font-weight: 500;
}

/*
  type-tag：照片类型编号标签
  蓝色小方块，紧跟在"车头照"等文字前面，
  编号对应系统中的 photo_type 枚举值。
*/
.type-tag {
  display: inline-block;
  background: #409eff;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  margin-right: 4px;
  font-weight: 700;
}

/* ========== 货物照区域（右侧栏） ========== */

/* 右侧栏独立滚动 */
.goods-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-photo-area {
  flex: 1;
  max-height: 690px; /* 超过400px显示滚动条 */
  padding: 16px;
  overflow-y: auto;
}

/* 货物照竖向列表 */
.goods-photo-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.goods-photo-list::-webkit-scrollbar {
  width: 4px;
}
.goods-photo-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 2px;
}

/* 每张货物照：图片 + 标签 */
.goods-photo-item {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.goods-photo-img {
  width: 100%;
  height: 150px;
  display: block;
  cursor: zoom-in;
  background: #f5f7fa;
}

.goods-photo-label {
  text-align: center;
  font-size: 13px;
  color: #909399;
  padding: 8px 0;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
  font-weight: 500;
}

/* 无货物照时的占位 */
.goods-photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 200px;
  color: #d0d0d0;
  font-size: 13px;
}

/* ========== 数据网格区域（4列） ========== */

/* 5列等宽网格 */
.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

/* 每列：右边框分隔，内容从上到下排列 */
.data-col {
  border-right: 1px solid #ebeef5;
  padding: 0;
}

.data-col:last-child {
  border-right: none;
}

/* 每行：label 靠左，value 靠右，中间有间距 */
.data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  border-bottom: 1px solid #f5f5f5;
  min-height: 44px;
}

.data-row:last-child {
  border-bottom: none;
}

/* 标签：深色加粗 */
.data-label {
  font-size: 14px;
  color: #505050;
  flex-shrink: 0;
  font-weight: 600;
}

/* 数值：深色粗体 */
.data-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
  text-align: right;
  word-break: break-all;
  margin-left: 8px;
}

/* 单位后缀：更小的灰色字 */
.data-value .unit {
  font-size: 13px;
  color: #909399;
}

/* 车种名称：蓝色高亮 */
.data-value.primary {
  color: #409eff;
  font-weight: 700;
}

/* 编号/ID：等宽字体，适合长字符串 */
.data-value.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #606266;
}

/* 金额：橙色大字体突出 */
.data-value.money {
  color: #e6a23c;
  font-weight: 700;
  font-size: 16px;
}

/* 车牌号：等宽粗体突出 */
.data-value.plate {
  font-weight: 700;
  font-family: 'Consolas', 'Monaco', monospace;
  letter-spacing: 1px;
  color: #303133;
}

/* ========== 底部结论栏 ========== */

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3e1 100%);
  border-top: 3px solid #67c23a;
  margin-top: 12px;
}

.result-bar-main {
  flex-shrink: 0;
}

.result-badge {
  font-size: 20px;
  font-weight: 800;
  padding: 10px 28px;
  letter-spacing: 3px;
}

.result-bar-items {
  display: flex;
  align-items: center;
  gap: 40px;
  flex: 1;
  justify-content: flex-end;
}

.result-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.result-label {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.result-value {
  font-size: 16px;
  color: #303133;
  font-weight: 700;
}

.result-value.danger {
  color: #f56c6c;
}

/* ========== 结论区域（已废弃） ========== */

/* 2列网格：左2右1 */
.conclusion-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0;
}

.conclusion-card {
  padding: 12px;
  border-right: 1px solid #ebeef5;
}

.conclusion-card:last-child {
  border-right: none;
}

/* 结论区小标题 */
.conclusion-card-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

/*
  查验结果大字：28px 超大字号，居中显示
  三种状态对应三种配色方案
*/
.conclusion-big-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  text-align: center;
  padding: 8px 0;
  border-radius: 6px;
  margin-bottom: 6px;
}

/* 合格：绿色背景+绿色文字 */
.conclusion-big-text.pass {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #c2e7b0;
}

/* 不合格：红色背景+红色文字 */
.conclusion-big-text.fail {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

/* 待查验：灰色背景+灰色文字 */
.conclusion-big-text.pending {
  background: #f4f4f5;
  color: #909399;
  border: 1px solid #d3d4d6;
}

/* 结论提示语：图标+文字居中 */
.conclusion-card-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  justify-content: center;
}

/* 不合格类型文本 */
.reason-text {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  padding: 12px 0;
  text-align: center;
  background: #f5f7fa;
  border-radius: 6px;
}

.reason-text.danger {
  color: #f56c6c; /* 不合格用红色 */
  background: #fef0f0;
}

.reason-text.disabled {
  color: #c0c4cc; /* 占位用浅灰 */
  font-weight: 400;
  font-size: 14px;
}

/* 不合格编号：红色等宽大字 */
.nopass-code {
  font-size: 20px;
  font-weight: 800;
  color: #f56c6c;
  letter-spacing: 2px;
  text-align: center;
  padding: 6px 0;
  font-family: 'Consolas', 'Monaco', monospace;
}

.nopass-code.disabled {
  color: #dcdfe6;
  font-size: 16px;
}

/* 操作员信息 */
.operator-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.operator-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.op-label {
  font-size: 12px;
  color: #909399;
}

.op-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

/* ========== 响应式：小屏适配 ========== */
@media (max-width: 900px) {
  /* 两栏布局改为单列：右侧栏沉底 */
  .detail-body {
    flex-direction: column;
  }

  .detail-right {
    max-width: 100%;
    width: 100%;
    border-top: 1px solid #e4e7ed;
    border-right: none;
  }

  /* 证据照6列变3列 */
  .evidence-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  /* 数据网格4列变2列 */
  .data-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  /* 结论区3列变1列 */
  .conclusion-grid {
    grid-template-columns: 1fr;
  }
}

/* ========== 货物类型字段 ========== */
.goods-type-field {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}

/* ========== 货物类型弹窗 ========== */
.goods-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 8px;
}

.goods-dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.temp-selected {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 12px;
  background: #f0f9eb;
  border-radius: 4px;
  margin-bottom: 12px;
  max-height: 80px;
  overflow-y: auto;
}

.temp-label {
  font-size: 12px;
  color: #67c23a;
  font-weight: 600;
  margin-right: 4px;
  flex-shrink: 0;
}

.variety-card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  max-height: 480px;
  overflow-y: auto;
  padding: 4px;
}

.variety-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  display: flex;
  align-items: center;
}

.variety-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.variety-card.selected {
  border-color: #67c23a;
  background: #f0f9eb;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.card-type {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.card-aliases {
  font-size: 11px;
  color: #a0a8b6;
  line-height: 1.4;
}

.card-left {
  flex: 1;
  min-width: 0;
}

.card-icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #f5f7fa;
  margin-left: 8px;
  overflow: hidden;
}

.variety-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.no-data {
  grid-column: 1 / -1;
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
}

.dialog-footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.selected-count {
  font-size: 13px;
  color: #909399;
}

/* ========== 底部按钮 ========== */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
