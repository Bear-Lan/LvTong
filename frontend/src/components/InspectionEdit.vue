<template>
  <!--
    新增/编辑查验记录弹窗
    - isEdit=false：新增模式（title="新增查验记录"）
    - isEdit=true：编辑模式（title="编辑查验记录"）
    - destroy-on-close：关闭弹窗时销毁 DOM，避免数据残留
  -->
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑查验记录' : '新增查验记录'"
    width="800px"
    destroy-on-close
    class="inspection-edit-dialog"
  >
    <!--
      表单验证：
      - el-form：绑定 model（form 对象）和 rules（验证规则）
      - ref="formRef"：用于在 JS 中调用 validate() 手动触发表单校验
    -->
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="edit-form"
    >
      <!-- ========== 基本信息 ========== -->
      <div class="form-section">
        <div class="form-section-title">
          <span class="section-dot blue"></span>基本信息
        </div>
        <el-row :gutter="20">
          <!-- 车牌号码 - 必填 -->
          <el-col :span="8">
            <el-form-item label="车牌号码" prop="plateNumber">
              <el-input v-model="form.plateNumber" placeholder="请输入车牌号码" />
            </el-form-item>
          </el-col>
          <!-- 车牌颜色（编辑时只读） -->
          <el-col :span="8">
            <el-form-item label="车牌颜色" prop="plateNumberGc">
              <el-select v-model="form.plateNumberGc" placeholder="请选择" clearable :disabled="isEdit" style="width: 100%;">
                <el-option label="蓝色" value="蓝" />
                <el-option label="黄色" value="黄" />
                <el-option label="绿色" value="绿" />
                <el-option label="黄绿双拼" value="黄绿" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 司机电话 -->
          <el-col :span="8">
            <el-form-item label="司机电话" prop="driverPhone">
              <el-input v-model="form.driverPhone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 车辆类型 -->
          <el-col :span="8">
            <el-form-item label="车辆类型" prop="vehicleType">
              <el-select v-model="form.vehicleType" placeholder="请选择" clearable style="width: 100%;">
                <el-option label="一型货车" value="11" />
                <el-option label="二型货车" value="12" />
                <el-option label="三型货车" value="13" />
                <el-option label="四型货车" value="14" />
                <el-option label="五型货车" value="15" />
                <el-option label="六型货车" value="16" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 货箱类型 -->
          <el-col :span="8">
            <el-form-item label="货箱类型" prop="vehicleContainertype">
              <el-select v-model="form.vehicleContainertype" placeholder="请选择" clearable style="width: 100%;">
                <el-option label="罐式货车" value="1" />
                <el-option label="敞篷货车（平板式）" value="2.1" />
                <el-option label="敞篷货车（栅栏式）" value="2.2" />
                <el-option label="普通货车(篷布包裹式)" value="3.1" />
                <el-option label="厢式货车(封闭货车)" value="4.1" />
                <el-option label="特殊结构货车(水箱式)" value="5.1" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 通行介质（编辑时只读） -->
          <el-col :span="8">
            <el-form-item label="通行介质" prop="passcodeMediaType">
              <el-select v-model="form.passcodeMediaType" placeholder="请选择" clearable :disabled="isEdit" style="width: 100%;">
                <el-option label="OBU" value="1" />
                <el-option label="CPC卡" value="2" />
                <el-option label="纸券" value="3" />
                <el-option label="M1卡" value="4" />
                <el-option label="无通行介质" value="9" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- ========== 货物信息 ========== -->
      <div class="form-section">
        <div class="form-section-title">
          <span class="section-dot green"></span>货物信息
        </div>
        <el-row :gutter="20">
          <!-- 货物类型：点击弹出表单选择 -->
          <el-col :span="24">
            <el-form-item label="货物类型" prop="goodsType">
              <!-- 已选品种标签 + 选择按钮 -->
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
            </el-form-item>
          </el-col>
          <!-- 货物小类别（编辑时只读） -->
          <el-col :span="8">
            <el-form-item label="货物小类别" prop="goodsCategory">
              <el-input v-model="form.goodsCategory" placeholder="如：叶菜类、瓜类" disabled />
            </el-form-item>
          </el-col>
          <!-- 满载率 -->
          <el-col :span="8">
            <el-form-item label="满载率(%)" prop="loadRate">
              <el-input-number v-model="form.loadRate" :min="0" :max="100" :precision="2" style="width: 100%;" placeholder="0-100" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 车辆尺寸 -->
          <el-col :span="24">
            <el-form-item label="车辆尺寸(长×宽×高)" prop="vehicleSize">
              <el-input v-model="form.vehicleSize" placeholder="如：18000×2500×4000（单位：毫米）" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- ========== 查验信息 ========== -->
      <div class="form-section">
        <div class="form-section-title">
          <span class="section-dot orange"></span>查验信息
        </div>
        <el-row :gutter="20">
          <!-- 查验结果 -->
          <el-col :span="8">
            <el-form-item label="查验结果" prop="resultStatus">
              <el-select v-model="form.resultStatus" placeholder="请选择" style="width: 100%;">
                <el-option label="待查验" :value="0" />
                <el-option label="合格" :value="1" />
                <el-option label="不合格" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 不合格类型：仅在 resultStatus=2 时填写 -->
          <el-col :span="8">
            <el-form-item label="不合格类型" prop="nopassType">
              <el-select v-model="form.nopassType" placeholder="合格则不填" clearable style="width: 100%;">
                <el-option label="车货总质量超限" :value="11" />
                <el-option label="外廓尺寸超限" :value="12" />
                <el-option label="货物非《目录》内" :value="13" />
                <el-option label="货物属深加工产品" :value="14" />
                <el-option label="货物冷冻发硬/腐烂/变质" :value="15" />
                <el-option label="未达80%装载" :value="18" />
                <el-option label="混装非鲜活农产品" :value="19" />
                <el-option label="假冒绿通" :value="21" />
                <el-option label="未提供行驶证原件" :value="22" />
                <el-option label="行驶证过期" :value="24" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 操作员 -->
          <el-col :span="8">
            <el-form-item label="操作员" prop="operatorName">
              <el-input v-model="form.operatorName" placeholder="查验操作员姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 复核（下拉：姓名+电话） -->
          <el-col :span="8">
            <el-form-item label="复核" prop="reviewerPhone">
              <el-select
                v-model="form.reviewerPhone"
                placeholder="请选择核验员"
                clearable
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="r in reviewers"
                  :key="r.phone"
                  :label="r.realName + ' ' + r.phone"
                  :value="r.phone"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 班组（手动填写） -->
          <el-col :span="8">
            <el-form-item label="班组" prop="groupId">
              <el-input v-model="form.groupId" placeholder="请输入班组" clearable style="width: 100%;" />
            </el-form-item>
          </el-col>
          <!-- 查验时间（编辑时只读） -->
          <el-col :span="8">
            <el-form-item label="查验时间" prop="inspectionTime">
              <el-date-picker
                v-model="form.inspectionTime"
                type="datetime"
                placeholder="选择查验时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled="isEdit"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <!-- 历史记录备注 -->
          <el-col :span="24">
            <el-form-item label="备注内容" prop="historyRecord">
              <el-input v-model="form.historyRecord" type="textarea" :rows="2" placeholder="历史查验记录备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- ========== 通行码信息（编辑时只读） ========== -->
      <div class="form-section" :class="{ 'read-only': isEdit }">
        <div class="form-section-title">
          <span class="section-dot purple"></span>通行码信息
          <span v-if="isEdit" class="readonly-hint">（只读）</span>
        </div>
        <el-row :gutter="20">
          <!-- 入口站编号 -->
          <el-col :span="8">
            <el-form-item label="入口站编号" prop="passcodeEnStationId">
              <el-input v-model="form.passcodeEnStationId" placeholder="入口收费站点编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 出口站编号 -->
          <el-col :span="8">
            <el-form-item label="出口站编号" prop="passcodeExStationId">
              <el-input v-model="form.passcodeExStationId" placeholder="出口收费站点编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 通行颜色 -->
          <el-col :span="8">
            <el-form-item label="通行颜色" prop="passcodeVehicleColorName">
              <el-select v-model="form.passcodeVehicleColorName" placeholder="请选择" clearable :disabled="isEdit" style="width: 100%;">
                <el-option label="绿通" value="1" />
                <el-option label="非绿通" value="2" />
                <el-option label="ETC" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 入口重量 -->
          <el-col :span="8">
            <el-form-item label="入口重量(KG)" prop="passcodeEnWeight">
              <el-input v-model="form.passcodeEnWeight" placeholder="入口称重(kg)" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 出口重量 -->
          <el-col :span="8">
            <el-form-item label="出口重量(KG)" prop="passcodeExWeight">
              <el-input v-model="form.passcodeExWeight" placeholder="出口称重(kg)" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 出口交易时间 -->
          <el-col :span="8">
            <el-form-item label="出口交易时间" prop="passcodeExTime">
              <el-input v-model="form.passcodeExTime" placeholder="格式：2026-03-24T12:30:00" :disabled="isEdit" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 通行标识ID -->
          <el-col :span="8">
            <el-form-item label="通行标识ID" prop="passcodePassId">
              <el-input v-model="form.passcodePassId" placeholder="车辆通行唯一编号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 出口交易编号 -->
          <el-col :span="8">
            <el-form-item label="出口交易编号" prop="passcodeTransactionId">
              <el-input v-model="form.passcodeTransactionId" placeholder="交易流水号" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 通行费 -->
          <el-col :span="8">
            <el-form-item label="通行费(元)" prop="passcodeFee">
              <el-input-number v-model="form.passcodeFee" :min="0" :precision="2" style="width: 100%;" placeholder="总金额" :disabled="isEdit" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 应收金额 -->
          <el-col :span="8">
            <el-form-item label="应收金额(元)" prop="passcodePayFee">
              <el-input-number v-model="form.passcodePayFee" :min="0" :precision="2" style="width: 100%;" placeholder="实付金额" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 通行省份数量 -->
          <el-col :span="8">
            <el-form-item label="通行省份数量" prop="passcodeProvinceCount">
              <el-input v-model="form.passcodeProvinceCount" placeholder="通行省份数" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <!-- 交易支付方式 -->
          <el-col :span="8">
            <el-form-item label="交易支付方式" prop="passcodeTransPayType">
              <el-select v-model="form.passcodeTransPayType" placeholder="请选择" clearable :disabled="isEdit" style="width: 100%;">
                <el-option label="出口ETC通行" value="1" />
                <el-option label="出口ETC刷卡" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <!-- 车辆状态标识 -->
          <el-col :span="8">
            <el-form-item label="车辆状态标识" prop="passcodeVehicleSign">
              <el-select v-model="form.passcodeVehicleSign" placeholder="请选择" clearable :disabled="isEdit" style="width: 100%;">
                <el-option label="绿通车" value="0x02" />
                <el-option label="收割机" value="0x03" />
                <el-option label="默认" value="0xFF" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <!-- ========== 图片上传（编辑时只读） ========== -->
      <div class="form-section" :class="{ 'read-only': isEdit }">
        <div class="form-section-title">
          <span class="section-dot teal"></span>图片上传
          <span class="section-hint">（点击按钮上传，全部可选）</span>
          <span v-if="isEdit" class="readonly-hint">（只读）</span>
        </div>

        <!-- ===== 单张图片字段（6列一行） ===== -->
        <el-row :gutter="20">
          <el-col v-for="field in singleImageFields" :key="field.key" :span="4">
            <el-form-item :label="field.label">
              <div class="img-upload-item">
                <!-- 已有图片：显示缩略图 -->
                <template v-if="form[field.key]">
                  <div class="img-preview">
                    <el-image
                      :src="formatImgUrl(form[field.key])"
                      fit="contain"
                      class="preview-img"
                      :preview-src-list="[formatImgUrl(form[field.key])]"
                    />
                    <div class="preview-actions">
                      <el-button link type="danger" size="small" @click="form[field.key] = ''" :disabled="isEdit">
                        <el-icon><Delete /></el-icon> 移除
                      </el-button>
                    </div>
                  </div>
                </template>
                <!-- 未上传：显示上传按钮 -->
                <template v-else>
                  <el-upload
                    ref="uploadRefs"
                    action="#"
                    :show-file-list="false"
                    :disabled="isEdit || uploading[field.key]"
                    :before-upload="(f) => beforeImgUpload(f, field.key)"
                    :http-request="(opt) => handleSingleUpload(opt, field.key)"
                  >
                    <el-button type="primary" plain size="small" :loading="uploading[field.key]" :disabled="isEdit">
                      <el-icon><Plus /></el-icon>
                      {{ uploading[field.key] ? '上传中...' : '上传' }}
                    </el-button>
                  </el-upload>
                </template>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ===== 货物照片（多张） ===== -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="货物照片（可多张）">
              <div class="img-upload-item goods-upload">
                <!-- 已有图片网格 -->
                <div v-if="goodsImageList.length > 0" class="goods-preview-grid">
                  <div v-for="(p, idx) in goodsImageList" :key="idx" class="goods-thumb">
                    <el-image :src="formatImgUrl(p)" fit="cover" class="thumb-img"
                      :preview-src-list="goodsImageList.map(x => formatImgUrl(x))" :initial-index="idx" />
                    <el-button link type="danger" size="small" class="thumb-del" @click="removeGoodsImg(idx)" :disabled="isEdit">
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                </div>
                <!-- 上传按钮 -->
                <el-upload
                  ref="uploadRefs"
                  action="#"
                  :show-file-list="false"
                  :disabled="isEdit || uploading.goods"
                  :before-upload="(f) => beforeImgUpload(f, 'goods')"
                  :http-request="(opt) => handleSingleUpload(opt, 'goods')"
                >
                  <el-button type="primary" plain :loading="uploading.goods" :disabled="isEdit">
                    <el-icon><Plus /></el-icon>
                    {{ uploading.goods ? '上传中...' : '添加货物照' }}
                  </el-button>
                </el-upload>
                <div class="upload-hint">支持多张，点击图片可放大预览</div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-form>

    <!-- 货物类型选择弹窗 -->
    <el-dialog
      v-model="goodsDialogVisible"
      title="选择货物类型"
      width="820px"
      destroy-on-close
      class="goods-dialog"
    >
      <div class="goods-dialog-toolbar">
        <!-- 产品大类下拉 -->
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
        <!-- 具体类别下拉 -->
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
        <!-- 品种名称搜索 -->
        <el-input
          v-model="filterVarietyName"
          placeholder="搜索品种名称"
          clearable
          size="default"
          style="flex: 1; min-width: 160px;"
        />
      </div>

      <!-- 已选品种标签 -->
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

      <!-- 品种卡片网格 -->
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

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * InspectionEdit 组件
 *
 * 【功能说明】
 * 统一的 新增 / 编辑 查验记录弹窗组件。
 * 通过 props.isEdit 区分模式，通过 props.row 传入待编辑数据。
 *
 * 【双向数据绑定】
 * - modelValue (v-model)：控制弹窗显示/隐藏（父组件传入）
 * - isEdit (props)：true=编辑模式，false=新增模式
 * - row (props)：编辑时传入的原始记录数据
 *
 * 【emit 事件】
 * - update:modelValue：关闭弹窗时通知父组件更新 v-model
 * - refresh：操作成功后通知父组件刷新列表
 *
 * 【表单数据流】
 * 1. 弹窗打开时（visible=true），watch 监听 row 变化
 * 2. 编辑模式（isEdit=true）：将 row 数据同步到 form
 * 3. 新增模式（isEdit=false）：调用 resetForm() 清空表单
 * 4. 提交时：仅提交 form 中有值的字段（过滤 null 和空字符串）
 *    → 实现后端的"部分更新"语义
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Picture, Delete, Close } from '@element-plus/icons-vue'
import { createInspection, updateInspection, getProductList } from '@/api/vehicleInspection'
import { getUserPhoneList } from '@/api/user'
import { uploadImage } from '@/api/upload'

// 批量导入品种图片
const varietyImages = import.meta.glob('@/assets/variety_img/*.png', { eager: true })

/**
 * 根据品种名称获取对应的图片
 * @param {string} varietyName - 品种名称
 * @returns {string|null} 图片 URL 或 null
 */
const getVarietyImage = (varietyName) => {
  if (!varietyName) return null
  // 直接匹配：图片名称 = 品种名称
  let imagePath = varietyImages[`/src/assets/variety_img/${varietyName}.png`]?.default
  if (imagePath) return imagePath
  // 模糊匹配：尝试匹配图片名称包含品种名称的情况
  for (const path in varietyImages) {
    const fileName = path.split('/').pop().replace('.png', '')
    if (fileName.includes(varietyName) || varietyName.includes(fileName)) {
      return varietyImages[path].default
    }
  }
  return null
}

// ================================================================
// Props & Emits
// ================================================================

/**
 * modelValue：控制弹窗显示/隐藏（与父组件 v-model 绑定）
 * row：编辑时传入的原始记录；新增时为空对象 {}
 * isEdit：true=编辑模式，false=新增模式
 */
const props = defineProps({
  modelValue: Boolean,
  row: { type: Object, default: () => ({}) },
  isEdit: { type: Boolean, default: false }
})

/**
 * update:modelValue：关闭时通知父组件
 * refresh：操作成功后通知父组件刷新表格
 */
const emit = defineEmits(['update:modelValue', 'refresh'])

// ================================================================
// 弹窗显示状态（computed 实现双向绑定）
// ================================================================

/**
 * visible：控制 el-dialog 的显示
 * - get: 读取父组件传入的 modelValue
 * - set: 通过 emit 通知父组件更新
 */
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// ================================================================
// 表单引用与状态
// ================================================================

/** 表单 DOM 引用，用于调用 validate() 触发表单校验 */
const formRef = ref(null)
/** 提交按钮 loading 状态，防止重复提交 */
const submitting = ref(false)

/** 货物类型选中值（数组，多个用 | 分隔存储） */
const selectedProducts = ref([])
/** 货物类型弹窗状态 */
const goodsDialogVisible = ref(false)
/** 弹窗内临时已选（确认前生效） */
const tempSelected = ref([])
/** 弹窗筛选：产品大类 */
const filterProductType = ref('')
/** 弹窗筛选：具体类别 */
const filterCategory = ref('')
/** 弹窗筛选：品种名称关键字 */
const filterVarietyName = ref('')

// ================================================================
// 货物类型下拉选项加载
// ================================================================

/** 缓存所有品种数据 */
let allVarieties = []
/** 产品大类列表（从接口动态获取） */
const productTypeOptions = ref([])
/** 所有具体类别列表（从接口动态获取） */
const allCategories = ref([])

/** 核验员列表（从接口动态获取） */
const reviewers = ref([])

// ================================================================
// 图片上传
// ================================================================

/** 单张图片字段配置 */
const singleImageFields = [
  { key: 'headImagePath',      label: '车头照片' },
  { key: 'tailImagePath',      label: '车尾照片' },
  { key: 'licenseImagePath',   label: '行驶证照片' },
  { key: 'bodyImagePath',      label: '车身照片' },
  { key: 'transparentImagePath', label: '透视影像' },
  { key: 'passcodeImagePath',  label: '通行凭证' },
]

/** 各字段上传 loading 状态 */
const uploading = reactive({})

/** 货物照路径数组（内存中维护，提交时合并为逗号分隔字符串） */
const goodsImageList = ref([])

/** 转换本地路径为后端图片接口 URL */
const formatImgUrl = (path) => {
  if (!path) return ''
  return `/api/images?path=${encodeURIComponent(path)}`
}

/** 上传前校验文件类型和大小 */
const beforeImgUpload = (file, fieldKey) => {
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/bmp', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持上传 jpg/png/gif/bmp/webp 格式的图片')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 20MB')
    return false
  }
  uploading[fieldKey] = true
  return true
}

/** 处理单张图片上传（自定义 http-request 模式） */
const handleSingleUpload = async ({ file }, fieldKey) => {
  try {
    const res = await uploadImage(file, fieldKey === 'transparentImagePath' ? 'transparent' : fieldKey === 'passcodeImagePath' ? 'passcode' : fieldKey)
    if (res.code === 200) {
      if (fieldKey === 'goods') {
        // 货物照追加到列表
        goodsImageList.value.push(res.data.path)
      } else {
        // 单张字段直接赋值
        form[fieldKey] = res.data.path
      }
    } else {
      ElMessage.error(res.message || '图片上传失败')
      return  // 上传失败时不写入路径，避免写入 undefined 或垃圾数据
    }
  } catch {
    ElMessage.error('图片上传失败，请重试')
  } finally {
    uploading[fieldKey] = false
  }
}

/** 移除货物照列表中指定索引的图片 */
const removeGoodsImg = (index) => {
  goodsImageList.value.splice(index, 1)
}

/** 具体类别下拉选项（根据选中大类动态过滤） */
const categoryOptions = computed(() => {
  if (!filterProductType.value) return allCategories.value
  return [...new Set(
    allVarieties
      .filter(v => v.productType === filterProductType.value)
      .map(v => v.category)
      .filter(c => c)
  )].sort()
})

/** 当前展示的品种列表（三级筛选） */
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

/**
 * loadProducts：加载农产品品种数据
 * 1. 提取所有不重复的产品大类作为第一级下拉选项
 * 2. 缓存完整品种列表供后续过滤
 */
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

// ================================================================
// 货物类型弹窗相关
// ================================================================

/** 打开货物类型弹窗 */
const openGoodsDialog = () => {
  tempSelected.value = [...selectedProducts.value]
  filterProductType.value = ''
  filterCategory.value = ''
  filterVarietyName.value = ''
  goodsDialogVisible.value = true
}

/** 切换品种选中状态 */
const toggleTemp = (code) => {
  const idx = tempSelected.value.indexOf(code)
  if (idx >= 0) {
    tempSelected.value.splice(idx, 1)
  } else {
    tempSelected.value.push(code)
  }
}

/** 从临时已选中移除 */
const removeTemp = (code) => {
  tempSelected.value = tempSelected.value.filter(c => c !== code)
}

/** 确认弹窗选择 */
const confirmGoodsDialog = () => {
  selectedProducts.value = [...tempSelected.value]
  goodsDialogVisible.value = false
}

/**
 * 从已选品种中移除一项
 */
const removeProduct = (code) => {
  selectedProducts.value = selectedProducts.value.filter(c => c !== code)
}

/**
 * 根据 productCode 找到品种名称
 */
const getVarietyName = (code) => {
  const v = allVarieties.find(v => v.productCode === code)
  return v ? v.varietyName : code
}

/**
 * 解析 aliases JSON 字符串，返回逗号分隔的别名文字
 */
const getAliasesText = (aliasesJson) => {
  if (!aliasesJson) return ''
  try {
    const arr = JSON.parse(aliasesJson)
    return Array.isArray(arr) ? arr.join('、') : ''
  } catch {
    return ''
  }
}

// ================================================================
// 表单数据模型
// ================================================================

/**
 * form：表单数据对象，与 el-form 的 model 绑定
 * 所有字段初始化为默认值，新增时整体替换为空值。
 */
const form = reactive({
  // 基本信息
  plateNumber: '',
  plateNumberGc: '',
  driverPhone: '',
  vehicleType: '',
  vehicleContainertype: '',
  // 货物信息
  goodsType: '',
  goodsCategory: '',
  loadRate: null,
  vehicleSize: '',
  historyRecord: '',
  // 查验信息
  resultStatus: null,  // 0=待查验, 1=合格, 2=不合格
  nopassType: null,    // 仅 resultStatus=2 时填写
  operatorName: '',
  inspectionTime: '',
  groupId: '',
  reviewerPhone: '',
  // 通行码信息
  passcodeVehicleColorName: '',
  passcodeTransPayType: '',
  passcodeVehicleSign: '',
  passcodeEnStationId: '',
  passcodeExStationId: '',
  passcodeEnWeight: '',
  passcodeExWeight: '',
  passcodeExTime: '',
  passcodePassId: '',
  passcodeTransactionId: '',
  passcodeFee: null,
  passcodePayFee: null,
  passcodeProvinceCount: '',
  passcodeMediaType: '',
  // 图片路径（全部可选，与详细页一致）
  headImagePath: '',
  tailImagePath: '',
  licenseImagePath: '',
  goodsImagePath: '',
  bodyImagePath: '',
  transparentImagePath: '',
  passcodeImagePath: ''
})

// ================================================================
// 表单验证规则
// ================================================================

/**
 * rules：表单验证规则
 * 目前仅对 plateNumber 设置必填校验。
 * 其他字段若需要校验，可在对应 el-form-item 上添加 prop 属性。
 *
 * 触发时机：
 * - el-input/select 默认在 blur 时触发
 * - el-input-number 在 change 时触发
 */
const rules = {
  plateNumber: [
    { required: true, message: '请输入车牌号码', trigger: 'blur' }
  ]
}

// ================================================================
// 重置表单
// ================================================================

/**
 * resetForm：将表单恢复为默认值（新增模式使用）
 *
 * 【为什么不直接用 form = {...} 重新赋值？】
 * 因为 form 是用 reactive() 创建的响应式对象。
 * 直接赋值会丢失响应式能力（form 变量本身变成普通对象）。
 * 使用 Object.assign() 原地更新属性，可以保持 form 的响应式。
 *
 * 【loadRate/loadWeight 初始化为 null】
 * 前端不填时传 null，后端不更新该字段（保持原有值）。
 */
const resetForm = () => {
  Object.assign(form, {
    plateNumber: '',
    plateNumberGc: '',
    driverPhone: '',
    vehicleType: '',
    vehicleContainertype: '',
    goodsType: '',
    goodsCategory: '',
    loadRate: null,
    vehicleSize: '',
    historyRecord: '',
    resultStatus: null,
    nopassType: null,
    operatorName: '',
    inspectionTime: '',
    groupId: '',
    reviewerPhone: '',
    passcodeVehicleColorName: '',
    passcodeTransPayType: '',
    passcodeVehicleSign: '',
    passcodeEnStationId: '',
    passcodeExStationId: '',
    passcodeEnWeight: '',
    passcodeExWeight: '',
    passcodeExTime: '',
    passcodePassId: '',
    passcodeTransactionId: '',
    passcodeFee: null,
    passcodePayFee: null,
    passcodeProvinceCount: '',
    passcodeMediaType: '',
    headImagePath: '',
    tailImagePath: '',
    licenseImagePath: '',
    goodsImagePath: '',
    bodyImagePath: '',
    transparentImagePath: '',
    passcodeImagePath: ''
  })
}

// ================================================================
// 数据同步 watch
// ================================================================

/**
 * 监听 row 变化，同步数据到表单
 *
 * 【immediate: true 的必要性】
 * setup() 执行时，弹窗组件虽然不可见，但已经被挂载。
 * 加上 immediate: true 可以确保在 setup 阶段同步执行一次回调：
 * - 编辑模式：立即将 row 数据填充到表单
 * - 新增模式：立即重置表单为空值
 *
 * 【为什么 resetForm 要放在 watch 之前定义？】
 * 由于 JavaScript 的 TDZ（暂时性死区），const 声明的变量在声明行之前
 * 处于"已声明但未初始化"状态，访问会抛出 ReferenceError。
 * 加上 immediate: true 后，watch 回调在 setup 阶段同步执行。
 * 因此 resetForm 必须定义在 watch 之前，否则触发 TDZ 错误。
 *
 * 【为什么不直接 watch visible？】
 * 编辑时 row 在打开弹窗前就已传入，若编辑后再点新增，
 * row 仍然是上一次的值。此时 watch visible 不会触发（新值也是 true）。
 * 直接 watch row 更可靠。
 */
watch(() => props.row, (row) => {
  if (props.isEdit && row && row.id) {
    // 编辑模式：将 row 数据同步到 form（原地更新，保持响应式）
    Object.keys(form).forEach(key => {
      // 仅处理 row 中有值的字段
      if (row[key] !== undefined && row[key] !== null) {
        form[key] = row[key]
      }
    })
    // 同步 goodsType 到已选品种数组
    selectedProducts.value = row.goodsType
      ? row.goodsType.split('|').map(c => c.trim()).filter(c => c)
      : []
    // 同步货物照列表（支持中英文逗号分隔）
    goodsImageList.value = row.goodsImagePath
      ? row.goodsImagePath.split(/[,，]/).map(p => p.trim()).filter(p => p)
      : []
  } else if (!props.isEdit) {
    // 新增模式：重置所有字段
    resetForm()
    selectedProducts.value = []
    goodsImageList.value = []
    tempSelected.value = []
    filterProductType.value = ''
    filterCategory.value = ''
    filterVarietyName.value = ''
  }
}, { immediate: true })

// ================================================================
// 提交处理
// ================================================================

/**
 * handleSubmit：提交表单
 *
 * 【执行流程】
 * 1. 调用 formRef.value.validate() 触发表单校验
 *    - 校验失败：直接返回，不继续提交
 *    - 校验成功：继续执行后续逻辑
 * 2. 构建提交数据（仅包含有值的字段，过滤 null 和空字符串）
 *    → 实现后端部分更新语义
 * 3. 根据 isEdit 决定调用新增或更新 API
 * 4. 成功：提示用户 + 关闭弹窗 + 刷新父组件列表
 * 5. 失败：提示错误信息
 *
 * 【部分更新实现】
 * 前端提交时过滤掉所有 null 和空字符串字段，
 * 这样后端 Service.update() 中只有非 null 字段才会被更新，
 * 避免将 null 值覆盖掉数据库中的原有数据。
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  // 触发表单校验，valid=true 表示校验通过
  await formRef.value.validate(async (valid) => {
    if (!valid) return  // 校验失败，终止提交

    submitting.value = true
    try {
      // 同步货物类型：selectedProducts 数组 → goodsType 字符串
      if (selectedProducts.value && selectedProducts.value.length > 0) {
        form.goodsType = [...selectedProducts.value].join('|')
      } else {
        form.goodsType = ''
      }

      // 同步货物照列表：goodsImageList → goodsImagePath 逗号分隔字符串
      form.goodsImagePath = goodsImageList.value.join(',')

      // 构建提交数据：过滤 null 和空字符串
      const data = {}
      Object.keys(form).forEach(key => {
        const val = form[key]
        // 只保留有意义的值（排除 null 和空字符串）
        if (val !== null && val !== '' && val !== undefined) {
          data[key] = val
        }
      })

      // 根据模式调用不同 API
      let res
      if (props.isEdit) {
        if (!props.row?.id) {
          ElMessage.error('记录 ID 缺失，请重新打开编辑页面')
          return
        }
        res = await updateInspection(props.row.id, data)
      } else {
        res = await createInspection(data)
      }

      if (res.code === 200) {
        ElMessage.success(props.isEdit ? '修改成功' : '新增成功')
        visible.value = false          // 关闭弹窗
        emit('refresh')                // 通知父组件刷新列表
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch {
      ElMessage.error('操作失败，请重试')
    } finally {
      // finally 确保无论成功失败，最终都关闭 loading
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadProducts()
  loadReviewers()
})
</script>

<style scoped>
/* ========== 弹窗内边距调整 ========== */
.inspection-edit-dialog :deep(.el-dialog__body) {
  padding: 12px 24px 8px;
}

/* ========== 表单区域滚动 ========== */
.edit-form {
  max-height: 65vh;   /* 限制最大高度为视口的 65% */
  overflow-y: auto;  /* 超出时显示滚动条 */
  padding-right: 4px; /* 滚动条占位，防止内容被遮挡 */
}

/* 滚动条样式 */
.edit-form::-webkit-scrollbar {
  width: 4px;
}
.edit-form::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 2px;
}

/* ========== 分组区块 ========== */
.form-section {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

/* 最后一项去掉下边距 */
.form-section:last-child {
  margin-bottom: 0;
}

/* 分组标题 */
.form-section-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

/* 标题前的彩色圆点 */
.section-hint {
  font-size: 12px;
  font-weight: 400;
  color: #c0c4cc;
  margin-left: 8px;
}

.section-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;  /* 防止圆点被压缩 */
}
.section-dot.blue   { background: #409eff; }  /* 基本信息-蓝 */
.section-dot.green  { background: #67c23a; }  /* 货物信息-绿 */
.section-dot.orange { background: #e6a23c; }  /* 查验信息-橙 */
.section-dot.purple { background: #a855f7; }  /* 通行码信息-紫 */
.section-dot.teal   { background: #14b8a6; }  /* 图片路径-青 */

/* ========== 表单项间距收紧 ========== */
.edit-form :deep(.el-form-item) {
  margin-bottom: 0;   /* 去掉默认下边距，由 el-row 的 gutter 控制 */
}

.edit-form :deep(.el-form-item__label) {
  font-size: 12px;
  color: #909399;
  padding-bottom: 4px !important;
  font-weight: 500;
}

/* ========== 底部按钮 ========== */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* ========== 货物类型表单区域 ========== */
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
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  max-height: 360px;
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

/* ========== 只读区块样式 ========== */
.form-section.read-only {
  opacity: 0.65;
}

.readonly-hint {
  font-size: 12px;
  font-weight: 400;
  color: #c0c4cc;
  margin-left: 8px;
}

/* ========== 图片上传 ========== */
.img-upload-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 80px;
}

.upload-hint {
  font-size: 11px;
  color: #c0c4cc;
  line-height: 1.4;
}

/* 单张图片预览 */
.img-preview {
  position: relative;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.img-preview .preview-img {
  width: 100%;
  height: 100%;
}

.img-preview .preview-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.4);
  text-align: center;
  padding: 3px 0;
}

.img-preview .preview-actions .el-button {
  color: #fff;
  font-size: 12px;
}

/* 货物照多图预览网格 */
.goods-preview-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.goods-thumb {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.goods-thumb .thumb-img {
  width: 100%;
  height: 100%;
}

.goods-thumb .thumb-del {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  padding: 2px;
  line-height: 1;
  color: #fff;
}
</style>
