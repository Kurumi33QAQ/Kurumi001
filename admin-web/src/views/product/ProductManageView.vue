<template>
  <div class="page-card">
    <el-form :inline="true" :model="query" class="toolbar-left">
      <el-form-item label="商品名">
        <el-input v-model="query.name" placeholder="请输入商品名" clearable />
      </el-form-item>
      <el-form-item label="上架状态">
        <el-select v-model="query.publishStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="最低价">
        <el-input-number v-model="query.minPrice" :min="0" :precision="2" :step="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="最高价">
        <el-input-number v-model="query.maxPrice" :min="0" :precision="2" :step="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button v-permission="'pms:product:write'" type="primary" @click="openCreateDialog">新增商品</el-button>
        <el-button
          v-permission="'pms:product:write'"
          :disabled="!selectedIds.length"
          @click="handleBatchPublish(1)"
        >
          批量上架
        </el-button>
        <el-button
          v-permission="'pms:product:write'"
          :disabled="!selectedIds.length"
          @click="handleBatchPublish(0)"
        >
          批量下架
        </el-button>
        <el-button
          v-permission="'pms:product:write'"
          :disabled="!selectedIds.length"
          type="danger"
          plain
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
    </div>

    <el-table :data="list" border :loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="商品名" min-width="180" />
      <el-table-column prop="subTitle" label="副标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="价格" width="120">
        <template #default="{ row }">¥ {{ formatAmount(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="sale" label="销量" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">
            {{ row.publishStatus === 1 ? "上架" : "下架" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-permission="'pms:product:write'" link type="primary" @click="openEditDialog(row.id)">
            修改
          </el-button>
          <el-button v-permission="'pms:product:write'" link type="warning" @click="handleTogglePublish(row)">
            {{ row.publishStatus === 1 ? "下架" : "上架" }}
          </el-button>
          <el-button v-permission="'pms:product:write'" link type="danger" @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 14px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '修改商品' : '新增商品'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="88px">
        <el-form-item label="商品名" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="副标题" prop="subTitle">
          <el-input v-model="form.subTitle" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" :precision="0" />
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="image-uploader">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :before-upload="beforeSelectImage"
              :http-request="handleImageUpload"
            >
              <el-button :loading="uploadLoading">选择本地图片</el-button>
            </el-upload>
            <span class="upload-tip">
              支持 jpg/png/webp。未配置上传接口时仅本地预览，不会自动入库。
            </span>
          </div>
        </el-form-item>
        <el-form-item label="图片预览" v-if="previewImageUrl">
          <div class="preview-wrap">
            <el-image
              :src="previewImageUrl"
              :preview-src-list="[previewImageUrl]"
              fit="cover"
              class="preview-image"
            />
            <el-button text type="danger" @click="clearImage">移除图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="图片地址" prop="pic">
          <el-input v-model="form.pic" placeholder="可选：也可以手动填写线上 URL" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="商品详情" size="480px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="商品名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="副标题">{{ detail.subTitle || "-" }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥ {{ formatAmount(detail.price) }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
        <el-descriptions-item label="销量">{{ detail.sale }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.publishStatus === 1 ? "上架" : "下架" }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="图片地址">{{ detail.pic || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions } from "element-plus";
import {
  batchDeleteProductApi,
  batchUpdateProductPublishStatusApi,
  createProductApi,
  deleteProductApi,
  detailProductApi,
  listProductApi,
  updateProductApi,
  updateProductPublishStatusApi
} from "@/api/modules/product";
import { hasImageUploadApi, uploadImageApi } from "@/api/modules/upload";
import type { Product, ProductPayload } from "@/types/product";
import { formatAmount, formatDateTime } from "@/utils/format";

const loading = ref(false);
const list = ref<Product[]>([]);
const total = ref(0);
const selectedIds = ref<Array<string | number>>([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: "",
  publishStatus: undefined as number | undefined,
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined
});

const formVisible = ref(false);
const submitLoading = ref(false);
const uploadLoading = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<ProductPayload>({
  id: undefined,
  name: "",
  subTitle: "",
  price: 0.01,
  stock: 0,
  pic: ""
});

const localPreviewUrl = ref("");
const pendingLocalUpload = ref(false);
const previewImageUrl = computed(() => form.pic || localPreviewUrl.value);

const formRules: FormRules = {
  name: [{ required: true, message: "请输入商品名", trigger: "blur" }],
  price: [{ required: true, message: "请输入价格", trigger: "blur" }],
  stock: [{ required: true, message: "请输入库存", trigger: "blur" }]
};

const detailVisible = ref(false);
const detail = ref<Product | null>(null);

function cleanupLocalPreview() {
  if (localPreviewUrl.value.startsWith("blob:")) {
    URL.revokeObjectURL(localPreviewUrl.value);
  }
  localPreviewUrl.value = "";
}

function clearImage() {
  cleanupLocalPreview();
  pendingLocalUpload.value = false;
  form.pic = "";
}

const beforeSelectImage: UploadProps["beforeUpload"] = (rawFile) => {
  const isImage = rawFile.type.startsWith("image/");
  if (!isImage) {
    ElMessage.warning("请选择图片文件");
    return false;
  }
  const maxSizeMb = 5;
  const isLtMax = rawFile.size / 1024 / 1024 < maxSizeMb;
  if (!isLtMax) {
    ElMessage.warning(`图片大小不能超过 ${maxSizeMb}MB`);
    return false;
  }

  cleanupLocalPreview();
  localPreviewUrl.value = URL.createObjectURL(rawFile);
  pendingLocalUpload.value = true;
  return true;
};

async function handleImageUpload(options: UploadRequestOptions) {
  if (!hasImageUploadApi()) {
    ElMessage.warning("未配置图片上传接口，当前仅本地预览。请联系后端接入 OSS 上传接口。");
    return;
  }

  uploadLoading.value = true;
  try {
    const imageUrl = await uploadImageApi(options.file as File);
    form.pic = imageUrl;
    pendingLocalUpload.value = false;
    cleanupLocalPreview();
    ElMessage.success("图片上传成功");
    options.onSuccess?.({ url: imageUrl });
  } catch (error) {
    ElMessage.error((error as Error).message || "图片上传失败");
    options.onError?.(error as any);
  } finally {
    uploadLoading.value = false;
  }
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await listProductApi(query);
    list.value = data.records || [];
    total.value = data.total || 0;
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.name = "";
  query.publishStatus = undefined;
  query.minPrice = undefined;
  query.maxPrice = undefined;
  query.pageNum = 1;
  fetchList();
}

function handleSelectionChange(rows: Product[]) {
  selectedIds.value = rows.map((item) => item.id);
}

function resolveProductId(rawId: string | number | undefined) {
  if (rawId === undefined || rawId === null || rawId === "") {
    ElMessage.error("商品ID无效，请刷新页面后重试");
    return null;
  }
  if (typeof rawId === "number" && !Number.isSafeInteger(rawId)) {
    ElMessage.error("商品ID过大导致精度丢失，请将后端Long类型ID按字符串返回");
    return null;
  }
  return rawId;
}

function resetForm() {
  form.id = undefined;
  form.name = "";
  form.subTitle = "";
  form.price = 0.01;
  form.stock = 0;
  form.pic = "";
  pendingLocalUpload.value = false;
  cleanupLocalPreview();
}

function openCreateDialog() {
  resetForm();
  formVisible.value = true;
}

async function openEditDialog(id: string | number) {
  const safeId = resolveProductId(id);
  if (!safeId) return;
  try {
    const data = await detailProductApi(safeId);
    resetForm();
    form.id = data.id;
    form.name = data.name;
    form.subTitle = data.subTitle;
    form.price = Number(data.price);
    form.stock = data.stock;
    form.pic = data.pic;
    formVisible.value = true;
  } catch {
    // 错误提示由全局拦截器处理
  }
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate();
  if (pendingLocalUpload.value && !form.pic) {
    ElMessage.warning("本地图片尚未上传成功，请先完成上传或手动填写图片地址");
    return;
  }

  submitLoading.value = true;
  try {
    if (form.id) {
      await updateProductApi(form);
      ElMessage.success("商品修改成功");
    } else {
      await createProductApi(form);
      ElMessage.success("商品新增成功");
    }
    formVisible.value = false;
    fetchList();
  } finally {
    submitLoading.value = false;
  }
}

async function openDetail(id: string | number) {
  const safeId = resolveProductId(id);
  if (!safeId) return;
  try {
    detail.value = await detailProductApi(safeId);
    detailVisible.value = true;
  } catch {
    // 错误提示由全局拦截器处理
  }
}

async function handleTogglePublish(row: Product) {
  const safeId = resolveProductId(row.id);
  if (!safeId) return;
  try {
    const targetStatus = row.publishStatus === 1 ? 0 : 1;
    await updateProductPublishStatusApi(safeId, targetStatus);
    ElMessage.success(targetStatus === 1 ? "上架成功" : "下架成功");
    fetchList();
  } catch {
    // 错误提示由全局拦截器处理
  }
}

async function handleDelete(id: string | number) {
  const safeId = resolveProductId(id);
  if (!safeId) return;
  try {
    await ElMessageBox.confirm("确定删除该商品吗？删除后不可恢复。", "提示", { type: "warning" });
    await deleteProductApi(safeId);
    ElMessage.success("删除成功");
    fetchList();
  } catch {
    // 用户取消时不处理
  }
}

async function handleBatchPublish(publishStatus: number) {
  if (!selectedIds.value.length) {
    ElMessage.warning("请先选择商品");
    return;
  }
  const safeIds = selectedIds.value
    .map((id) => resolveProductId(id))
    .filter((id): id is string | number => id !== null);
  if (safeIds.length !== selectedIds.value.length) return;
  await batchUpdateProductPublishStatusApi(safeIds, publishStatus);
  ElMessage.success(publishStatus === 1 ? "批量上架成功" : "批量下架成功");
  fetchList();
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning("请先选择商品");
    return;
  }
  const safeIds = selectedIds.value
    .map((id) => resolveProductId(id))
    .filter((id): id is string | number => id !== null);
  if (safeIds.length !== selectedIds.value.length) return;
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个商品吗？`, "提示", {
      type: "warning"
    });
    await batchDeleteProductApi(safeIds);
    ElMessage.success("批量删除成功");
    fetchList();
  } catch {
    // 用户取消时不处理
  }
}

onMounted(fetchList);
onBeforeUnmount(cleanupLocalPreview);
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.upload-tip {
  color: #64748b;
  font-size: 12px;
}

.preview-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-image {
  width: 160px;
  height: 160px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
</style>
