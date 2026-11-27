<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { BenefitApi } from '#/api';

import { onMounted, reactive, ref } from 'vue';

import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';

import {
  createBenefitProductApi,
  deleteBenefitProductApi,
  getBenefitCardsApi,
  getBenefitClaimsApi,
  getBenefitProductsApi,
  importBenefitCardsApi,
  reviewBenefitClaimApi,
  updateBenefitProductApi,
} from '#/api';

const TabPane = Tabs.TabPane;
const TextArea = Input.TextArea;

const activeTab = ref('products');

const products = ref<BenefitApi.Product[]>([]);
const productsLoading = ref(false);

const cards = ref<BenefitApi.Card[]>([]);
const cardsTotal = ref(0);
const cardsLoading = ref(false);
const cardQuery = reactive({ page: 1, pageSize: 10, productSku: '', status: '' });

const claims = ref<BenefitApi.Claim[]>([]);
const claimsTotal = ref(0);
const claimsLoading = ref(false);
const claimQuery = reactive({
  memberId: undefined as number | undefined,
  page: 1,
  pageSize: 10,
  productSku: '',
  status: '',
});

const productOpen = ref(false);
const productSaving = ref(false);
const editingProduct = ref<BenefitApi.Product | null>(null);
const productForm = reactive({
  description: '',
  name: '',
  rules: '',
  skuCode: '',
  sort: 0,
  status: 1,
});

const importOpen = ref(false);
const importSaving = ref(false);
const importForm = reactive({
  batchNo: '',
  codesText: '',
  expireAt: undefined as Dayjs | undefined,
  productSku: '',
});

const reviewOpen = ref(false);
const reviewSaving = ref(false);
const reviewTarget = ref<BenefitApi.Claim | null>(null);
const reviewForm = reactive({ reason: '', result: 'SUCCESS' });

const cardStatusOptions = [
  { label: '未使用', value: 'UNUSED' },
  { label: '已领取', value: 'CLAIMED' },
  { label: '已停用', value: 'DISABLED' },
];
const claimStatusOptions = [
  { label: '待发放', value: 'AVAILABLE' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已发放', value: 'SUCCESS' },
  { label: '已失败', value: 'FAILED' },
  { label: '状态未知', value: 'UNKNOWN' },
];
const reviewResultOptions = [
  { label: '核实成功', value: 'SUCCESS' },
  { label: '核实失败并回退', value: 'FAILED' },
  { label: '人工补偿（不占名额）', value: 'COMPENSATED' },
];

const productColumns: TableColumnsType = [
  { dataIndex: 'skuCode', key: 'skuCode', title: 'SKU', width: 140 },
  { dataIndex: 'name', key: 'name', title: '商品名称', minWidth: 160 },
  { dataIndex: 'rules', key: 'rules', title: '兑换说明', minWidth: 240 },
  { dataIndex: 'sort', key: 'sort', title: '排序', width: 70 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'stock', key: 'stock', title: '可用库存', width: 140 },
  { key: 'action', title: '操作', width: 150, fixed: 'right' },
];

const cardColumns: TableColumnsType = [
  { dataIndex: 'productSku', key: 'productSku', title: 'SKU', width: 140 },
  { dataIndex: 'batchNo', key: 'batchNo', title: '批次', width: 160 },
  { dataIndex: 'codeMasked', key: 'codeMasked', title: '卡密（脱敏）', width: 160 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'expireAt', key: 'expireAt', title: '过期时间', width: 180 },
  { dataIndex: 'claimedAt', key: 'claimedAt', title: '领取时间', width: 180 },
];

const claimColumns: TableColumnsType = [
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员编号', width: 150 },
  { dataIndex: 'memberName', key: 'memberName', title: '会员', width: 100 },
  { dataIndex: 'periodNo', key: 'periodNo', title: '周期', width: 90 },
  { dataIndex: 'productName', key: 'productName', title: '权益商品', minWidth: 130 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'cardMasked', key: 'cardMasked', title: '卡密（脱敏）', width: 150 },
  { dataIndex: 'claimedAt', key: 'claimedAt', title: '领取时间', width: 170 },
  { dataIndex: 'remark', key: 'remark', title: '备注', minWidth: 160 },
  { key: 'action', title: '操作', width: 100, fixed: 'right' },
];

const cardPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});
const claimPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function cardStatusLabel(status: string) {
  return cardStatusOptions.find((item) => item.value === status)?.label ?? status;
}

function claimStatusLabel(status: string) {
  return claimStatusOptions.find((item) => item.value === status)?.label ?? status;
}

function claimStatusColor(status: string) {
  if (status === 'SUCCESS') return 'green';
  if (status === 'FAILED') return 'red';
  if (status === 'UNKNOWN') return 'orange';
  return 'blue';
}

async function loadProducts() {
  productsLoading.value = true;
  try {
    products.value = await getBenefitProductsApi();
  } finally {
    productsLoading.value = false;
  }
}

async function loadCards() {
  cardsLoading.value = true;
  try {
    const result = await getBenefitCardsApi({
      batchNo: cardQuery.batchNo || undefined,
      page: cardQuery.page,
      pageSize: cardQuery.pageSize,
      productSku: cardQuery.productSku || undefined,
      status: cardQuery.status || undefined,
    });
    cards.value = result.items;
    cardsTotal.value = result.total;
    cardPagination.total = result.total;
  } finally {
    cardsLoading.value = false;
  }
}

async function loadClaims() {
  claimsLoading.value = true;
  try {
    const result = await getBenefitClaimsApi({
      memberId: claimQuery.memberId || undefined,
      page: claimQuery.page,
      pageSize: claimQuery.pageSize,
      productSku: claimQuery.productSku || undefined,
      status: claimQuery.status || undefined,
    });
    claims.value = result.items;
    claimsTotal.value = result.total;
    claimPagination.total = result.total;
  } finally {
    claimsLoading.value = false;
  }
}

function openCreateProduct() {
  editingProduct.value = null;
  Object.assign(productForm, {
    description: '',
    name: '',
    rules: '',
    skuCode: '',
    sort: 0,
    status: 1,
  });
  productOpen.value = true;
}

function openEditProduct(record: BenefitApi.Product) {
  editingProduct.value = record;
  Object.assign(productForm, {
    description: record.description,
    name: record.name,
    rules: record.rules,
    skuCode: record.skuCode,
    sort: record.sort,
    status: record.status,
  });
  productOpen.value = true;
}

async function saveProduct() {
  if (!productForm.skuCode.trim() || !productForm.name.trim()) {
    message.warning('SKU 和商品名称必填');
    return;
  }
  productSaving.value = true;
  try {
    const payload = {
      description: productForm.description,
      name: productForm.name.trim(),
      rules: productForm.rules,
      skuCode: productForm.skuCode.trim(),
      sort: productForm.sort ?? 0,
      status: productForm.status,
    };
    if (editingProduct.value) {
      await updateBenefitProductApi(editingProduct.value.id, payload);
    } else {
      await createBenefitProductApi(payload);
    }
    message.success('保存成功');
    productOpen.value = false;
    await loadProducts();
  } finally {
    productSaving.value = false;
  }
}

function deleteProduct(record: BenefitApi.Product) {
  Modal.confirm({
    content: `删除或下架 ${record.skuCode}？已有卡密或领取单时会下架保留历史。`,
    onOk: async () => {
      await deleteBenefitProductApi(record.id);
      message.success('已处理');
      await loadProducts();
    },
    title: '确认删除',
  });
}

function openImport() {
  Object.assign(importForm, {
    batchNo: `B${dayjs().format('YYYYMMDDHHmm')}`,
    codesText: '',
    expireAt: dayjs().add(30, 'day'),
    productSku: products.value[0]?.skuCode ?? '',
  });
  importOpen.value = true;
}

async function submitImport() {
  const codes = importForm.codesText
    .split(/[\s,，]+/)
    .map((code) => code.trim())
    .filter(Boolean);
  if (!importForm.productSku || !importForm.batchNo.trim() || codes.length === 0) {
    message.warning('请选择 SKU、填写批次并粘贴卡密');
    return;
  }
  importSaving.value = true;
  try {
    const result = await importBenefitCardsApi({
      batchNo: importForm.batchNo.trim(),
      codes,
      expireAt: importForm.expireAt?.toISOString(),
      productSku: importForm.productSku,
    });
    message.success(`新增 ${result.imported} 张，重复 ${result.duplicated} 张`);
    importOpen.value = false;
    await Promise.all([loadProducts(), loadCards()]);
  } finally {
    importSaving.value = false;
  }
}

function openReview(record: BenefitApi.Claim) {
  reviewTarget.value = record;
  reviewForm.result = 'SUCCESS';
  reviewForm.reason = '';
  reviewOpen.value = true;
}

async function submitReview() {
  if (!reviewTarget.value || !reviewForm.reason.trim()) {
    message.warning('请填写核实原因');
    return;
  }
  reviewSaving.value = true;
  try {
    await reviewBenefitClaimApi(reviewTarget.value.id, {
      reason: reviewForm.reason.trim(),
      result: reviewForm.result as 'COMPENSATED' | 'FAILED' | 'SUCCESS',
    });
    message.success('已记录核实结果');
    reviewOpen.value = false;
    await Promise.all([loadClaims(), loadProducts()]);
  } finally {
    reviewSaving.value = false;
  }
}

function handleCardTableChange(page: any) {
  cardQuery.page = page.current;
  cardQuery.pageSize = page.pageSize;
  loadCards();
}

function handleClaimTableChange(page: any) {
  claimQuery.page = page.current;
  claimQuery.pageSize = page.pageSize;
  loadClaims();
}

function searchCards() {
  cardQuery.page = 1;
  loadCards();
}

function searchClaims() {
  claimQuery.page = 1;
  loadClaims();
}

onMounted(() => {
  loadProducts();
  loadCards();
  loadClaims();
});
</script>

<template>
  <div class="p-4">
    <Tabs v-model:active-key="activeTab">
      <TabPane key="products" tab="权益商品">
        <div class="mb-3 flex justify-end">
          <Button type="primary" @click="openCreateProduct">新增商品</Button>
        </div>
        <Table
          :columns="productColumns"
          :data-source="products"
          :loading="productsLoading"
          :pagination="false"
          :scroll="{ x: 1100 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="record.status === 1 ? 'green' : 'default'">
                {{ record.status === 1 ? '上架' : '下架' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'stock'">
              <Space>
                <span>{{ record.stock }}</span>
                <Tag v-if="record.lowStock" color="red">库存告警</Tag>
              </Space>
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button size="small" type="link" @click="openEditProduct(record)">
                  编辑
                </Button>
                <Button danger size="small" type="link" @click="deleteProduct(record)">
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="cards" tab="卡密">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Select
            v-model:value="cardQuery.productSku"
            :options="products.map((item) => ({ label: item.name, value: item.skuCode }))"
            allow-clear
            class="w-48"
            placeholder="SKU"
          />
          <Select
            v-model:value="cardQuery.status"
            :options="cardStatusOptions"
            allow-clear
            class="w-36"
            placeholder="状态"
          />
          <Input
            v-model:value="cardQuery.batchNo"
            allow-clear
            class="w-48"
            placeholder="批次号"
          />
          <Button @click="searchCards">查询</Button>
          <div class="flex-1"></div>
          <Button type="primary" @click="openImport">导入卡密</Button>
        </div>
        <Table
          :columns="cardColumns"
          :data-source="cards"
          :loading="cardsLoading"
          :pagination="cardPagination"
          :scroll="{ x: 900 }"
          row-key="id"
          size="middle"
          @change="handleCardTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="record.status === 'UNUSED' ? 'green' : 'default'">
                {{ cardStatusLabel(record.status) }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'expireAt'">
              {{ formatDate(record.expireAt) }}
            </template>
            <template v-else-if="column.key === 'claimedAt'">
              {{ formatDate(record.claimedAt) }}
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="claims" tab="领取单">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Select
            v-model:value="claimQuery.productSku"
            :options="products.map((item) => ({ label: item.name, value: item.skuCode }))"
            allow-clear
            class="w-48"
            placeholder="SKU"
          />
          <Select
            v-model:value="claimQuery.status"
            :options="claimStatusOptions"
            allow-clear
            class="w-36"
            placeholder="状态"
          />
          <InputNumber v-model:value="claimQuery.memberId" class="w-40" placeholder="会员 ID" />
          <Button @click="searchClaims">查询</Button>
        </div>
        <Table
          :columns="claimColumns"
          :data-source="claims"
          :loading="claimsLoading"
          :pagination="claimPagination"
          :scroll="{ x: 1200 }"
          row-key="id"
          size="middle"
          @change="handleClaimTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="claimStatusColor(record.status)">
                {{ claimStatusLabel(record.status) }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'claimedAt'">
              {{ formatDate(record.claimedAt) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Button size="small" type="link" @click="openReview(record)">
                核实
              </Button>
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>

    <Modal
      v-model:open="productOpen"
      :confirm-loading="productSaving"
      :title="editingProduct ? '编辑权益商品' : '新增权益商品'"
      @ok="saveProduct"
    >
      <div class="grid gap-3 py-2">
        <Input
          v-model:value="productForm.skuCode"
          :disabled="Boolean(editingProduct)"
          addon-before="SKU"
          placeholder="如 COFFEE_30"
        />
        <Input v-model:value="productForm.name" addon-before="名称" placeholder="商品名称" />
        <TextArea v-model:value="productForm.description" :rows="2" placeholder="商品描述" />
        <TextArea v-model:value="productForm.rules" :rows="3" placeholder="兑换说明/规则" />
        <div class="flex gap-3">
          <Select
            v-model:value="productForm.status"
            :options="[
              { label: '上架', value: 1 },
              { label: '下架', value: 0 },
            ]"
            class="w-32"
          />
          <InputNumber v-model:value="productForm.sort" :min="0" class="w-32" />
          <span class="self-center text-xs text-gray-500">排序（越小越前）</span>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="importOpen"
      :confirm-loading="importSaving"
      title="导入卡密"
      width="560px"
      @ok="submitImport"
    >
      <div class="grid gap-3 py-2">
        <Select
          v-model:value="importForm.productSku"
          :options="products.map((item) => ({ label: item.name, value: item.skuCode }))"
          placeholder="选择 SKU"
        />
        <Input v-model:value="importForm.batchNo" addon-before="批次" />
        <DatePicker
          v-model:value="importForm.expireAt"
          class="w-full"
          show-time
          placeholder="过期时间（可空）"
        />
        <TextArea
          v-model:value="importForm.codesText"
          :rows="6"
          placeholder="每行一张卡密，也可用空格/逗号分隔"
        />
        <p class="m-0 text-xs text-gray-500">
          卡密入库加密存储，页面只展示脱敏值，领取成功时向会员展示一次明文。
        </p>
      </div>
    </Modal>

    <Modal
      v-model:open="reviewOpen"
      :confirm-loading="reviewSaving"
      title="领取单人工核实"
      @ok="submitReview"
    >
      <div class="grid gap-3 py-2">
        <p class="m-0 text-sm text-gray-600">
          {{ reviewTarget?.memberNo }} {{ reviewTarget?.productName }}（{{
            claimStatusLabel(reviewTarget?.status ?? '')
          }}）
        </p>
        <Select v-model:value="reviewForm.result" :options="reviewResultOptions" />
        <TextArea
          v-model:value="reviewForm.reason"
          :rows="3"
          placeholder="必须填写原因，会写入操作审计"
        />
        <p class="m-0 text-xs text-gray-500">
          核实失败会释放名额并回退卡密；人工补偿不占本期名额，但会扣减可用库存。
        </p>
      </div>
    </Modal>
  </div>
</template>
