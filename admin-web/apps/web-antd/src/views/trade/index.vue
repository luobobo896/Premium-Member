<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { TradeApi } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import {
  Button,
  Descriptions,
  DescriptionsItem,
  Divider,
  Input,
  InputNumber,
  message,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createPlanApi,
  deletePlanApi,
  getOrderApi,
  getOrdersApi,
  getPaymentsApi,
  getPlansApi,
  refundOrderApi,
  updatePlanApi,
} from '#/api';

const TabPane = Tabs.TabPane;

const activeTab = ref('plans');

const plans = ref<TradeApi.Plan[]>([]);
const plansLoading = ref(false);
const planOpen = ref(false);
const planSaving = ref(false);
const editingPlan = ref<TradeApi.Plan | null>(null);
const planForm = reactive({
  aiImageQuota: 20,
  aiVideoQuota: 3,
  benefitQuota: 2,
  code: '',
  durationDays: 30,
  name: '',
  priceYuan: 59,
  status: 1,
});

const orders = ref<TradeApi.OrderRow[]>([]);
const ordersLoading = ref(false);
const orderQuery = reactive({
  memberId: undefined as number | undefined,
  orderNo: '',
  page: 1,
  pageSize: 10,
  status: '',
});
const orderPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<TradeApi.OrderDetail | null>(null);

const refundOpen = ref(false);
const refundSaving = ref(false);
const refundTarget = ref<TradeApi.OrderRow | null>(null);
const refundForm = reactive({
  amountYuan: 0,
  reason: '',
  result: 'APPROVE' as 'APPROVE' | 'REJECT',
});

const payments = ref<TradeApi.PaymentRow[]>([]);
const paymentsLoading = ref(false);
const paymentQuery = reactive({
  channel: '',
  orderNo: '',
  page: 1,
  pageSize: 10,
  txnNo: '',
});
const paymentPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

const orderStatusOptions = [
  { label: '待支付', value: 'CREATED' },
  { label: '已支付', value: 'PAID' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '已关闭', value: 'CLOSED' },
];

const planColumns: TableColumnsType = [
  { dataIndex: 'code', key: 'code', title: '套餐编码', width: 150 },
  { dataIndex: 'name', key: 'name', title: '名称', minWidth: 180 },
  { dataIndex: 'priceCents', key: 'priceCents', title: '价格', width: 100 },
  { dataIndex: 'durationDays', key: 'durationDays', title: '周期(天)', width: 90 },
  { dataIndex: 'aiImageQuota', key: 'aiImageQuota', title: '图片额度', width: 100 },
  { dataIndex: 'aiVideoQuota', key: 'aiVideoQuota', title: '视频额度', width: 100 },
  { dataIndex: 'benefitQuota', key: 'benefitQuota', title: '权益名额', width: 100 },
  { dataIndex: 'versionNo', key: 'versionNo', title: '版本', width: 90 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { key: 'action', title: '操作', width: 190, fixed: 'right' },
];

const orderColumns: TableColumnsType = [
  { dataIndex: 'orderNo', key: 'orderNo', title: '订单号', width: 230 },
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员编号', width: 150 },
  { dataIndex: 'planName', key: 'planName', title: '套餐', minWidth: 150 },
  { dataIndex: 'amountCents', key: 'amountCents', title: '金额', width: 100 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'channel', key: 'channel', title: '渠道', width: 100 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 170 },
  { key: 'action', title: '操作', width: 180, fixed: 'right' },
];

const paymentColumns: TableColumnsType = [
  { dataIndex: 'txnNo', key: 'txnNo', title: '交易号', width: 240 },
  { dataIndex: 'orderNo', key: 'orderNo', title: '订单号', width: 230 },
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员编号', width: 150 },
  { dataIndex: 'channel', key: 'channel', title: '渠道', width: 100 },
  { dataIndex: 'amountCents', key: 'amountCents', title: '金额', width: 100 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'paidAt', key: 'paidAt', title: '支付时间', width: 170 },
];

const detailColumns: TableColumnsType = [
  { dataIndex: 'txnNo', key: 'txnNo', title: '交易号', width: 220 },
  { dataIndex: 'channel', key: 'channel', title: '渠道', width: 90 },
  { dataIndex: 'amountCents', key: 'amountCents', title: '金额', width: 90 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'paidAt', key: 'paidAt', title: '支付时间', width: 160 },
];

const refundColumns: TableColumnsType = [
  { dataIndex: 'refundNo', key: 'refundNo', title: '退款单号', width: 220 },
  { dataIndex: 'amountCents', key: 'amountCents', title: '金额', width: 90 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'operator', key: 'operator', title: '操作人', width: 110 },
  { dataIndex: 'reason', key: 'reason', title: '原因', minWidth: 180 },
  { dataIndex: 'processedAt', key: 'processedAt', title: '处理时间', width: 160 },
];

function money(cents: number) {
  return `¥${(cents / 100).toFixed(2)}`;
}

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function statusColor(status: string) {
  if (status === 'PAID' || status === 'SUCCESS' || status === 'APPROVED') return 'green';
  if (status === 'REFUNDED' || status === 'REJECTED' || status === 'FAILED') return 'red';
  if (status === 'REFUNDING' || status === 'APPLIED') return 'orange';
  return 'blue';
}

const currentPlanCode = computed(() =>
  plans.value.find((item) => item.status === 1)?.code,
);

async function loadPlans() {
  plansLoading.value = true;
  try {
    plans.value = await getPlansApi();
  } finally {
    plansLoading.value = false;
  }
}

async function loadOrders() {
  ordersLoading.value = true;
  try {
    const result = await getOrdersApi({
      memberId: orderQuery.memberId || undefined,
      orderNo: orderQuery.orderNo || undefined,
      page: orderQuery.page,
      pageSize: orderQuery.pageSize,
      status: orderQuery.status || undefined,
    });
    orders.value = result.items;
    orderPagination.total = result.total;
  } finally {
    ordersLoading.value = false;
  }
}

async function loadPayments() {
  paymentsLoading.value = true;
  try {
    const result = await getPaymentsApi({
      channel: paymentQuery.channel || undefined,
      orderNo: paymentQuery.orderNo || undefined,
      page: paymentQuery.page,
      pageSize: paymentQuery.pageSize,
      txnNo: paymentQuery.txnNo || undefined,
    });
    payments.value = result.items;
    paymentPagination.total = result.total;
  } finally {
    paymentsLoading.value = false;
  }
}

function openCreatePlan() {
  editingPlan.value = null;
  Object.assign(planForm, {
    aiImageQuota: 20,
    aiVideoQuota: 3,
    benefitQuota: 2,
    code: '',
    durationDays: 30,
    name: '',
    priceYuan: 59,
    status: 1,
  });
  planOpen.value = true;
}

function openVersionPlan(record: TradeApi.Plan) {
  editingPlan.value = record;
  Object.assign(planForm, {
    aiImageQuota: record.aiImageQuota,
    aiVideoQuota: record.aiVideoQuota,
    benefitQuota: record.benefitQuota,
    code: record.code,
    durationDays: record.durationDays,
    name: record.name,
    priceYuan: record.priceCents / 100,
    status: 1,
  });
  planOpen.value = true;
}

async function savePlan() {
  if (!planForm.code.trim() || !planForm.name.trim()) {
    message.warning('套餐编码和名称必填');
    return;
  }
  if (planForm.priceYuan === null || planForm.priceYuan < 0) {
    message.warning('价格不能为负');
    return;
  }
  planSaving.value = true;
  try {
    const payload = {
      aiImageQuota: planForm.aiImageQuota ?? 20,
      aiVideoQuota: planForm.aiVideoQuota ?? 3,
      benefitQuota: planForm.benefitQuota ?? 2,
      code: planForm.code.trim(),
      durationDays: planForm.durationDays ?? 30,
      name: planForm.name.trim(),
      priceCents: Math.round(planForm.priceYuan * 100),
      status: planForm.status,
    };
    if (editingPlan.value) {
      await updatePlanApi(editingPlan.value.id, payload);
      message.success('已生成新版本，历史订单不受影响');
    } else {
      await createPlanApi(payload);
      message.success('套餐已保存');
    }
    planOpen.value = false;
    await loadPlans();
  } finally {
    planSaving.value = false;
  }
}

function disablePlan(record: TradeApi.Plan) {
  Modal.confirm({
    content: `下架套餐 ${record.code} v${record.versionNo}？历史订单仍保留该版本快照。`,
    onOk: async () => {
      await deletePlanApi(record.id);
      message.success('已下架');
      await loadPlans();
    },
    title: '确认下架',
  });
}

async function openDetail(record: TradeApi.OrderRow) {
  detailOpen.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    detail.value = await getOrderApi(record.orderNo);
  } finally {
    detailLoading.value = false;
  }
}

function openRefund(record: TradeApi.OrderRow) {
  refundTarget.value = record;
  refundForm.amountYuan = record.amountCents / 100;
  refundForm.reason = '';
  refundForm.result = 'APPROVE';
  refundOpen.value = true;
}

async function submitRefund() {
  if (!refundTarget.value || !refundForm.reason.trim()) {
    message.warning('请填写退款原因');
    return;
  }
  const amountCents = Math.round((refundForm.amountYuan ?? 0) * 100);
  if (amountCents <= 0 || amountCents > refundTarget.value.amountCents) {
    message.warning('退款金额必须在 0 与实付金额之间');
    return;
  }
  refundSaving.value = true;
  try {
    await refundOrderApi(refundTarget.value.orderNo, {
      amountCents,
      reason: refundForm.reason.trim(),
      result: refundForm.result,
    });
    message.success(refundForm.result === 'APPROVE' ? '退款成功，会员周期已终止' : '已驳回退款');
    refundOpen.value = false;
    await Promise.all([loadOrders(), loadPayments()]);
  } finally {
    refundSaving.value = false;
  }
}

function searchOrders() {
  orderQuery.page = 1;
  loadOrders();
}

function searchPayments() {
  paymentQuery.page = 1;
  loadPayments();
}

function onOrderTableChange(page: any) {
  orderQuery.page = page.current;
  orderQuery.pageSize = page.pageSize;
  loadOrders();
}

function onPaymentTableChange(page: any) {
  paymentQuery.page = page.current;
  paymentQuery.pageSize = page.pageSize;
  loadPayments();
}

onMounted(() => {
  loadPlans();
  loadOrders();
  loadPayments();
});
</script>

<template>
  <div class="p-4">
    <Tabs v-model:active-key="activeTab">
      <TabPane key="plans" tab="套餐管理">
        <div class="mb-3 flex items-center justify-between">
          <span class="text-xs text-gray-500">
            在售套餐：{{ currentPlanCode || '无' }}；修改价格或额度会生成新版本。
          </span>
          <Button type="primary" @click="openCreatePlan">新增套餐 / 新版本</Button>
        </div>
        <Table
          :columns="planColumns"
          :data-source="plans"
          :loading="plansLoading"
          :pagination="false"
          :scroll="{ x: 1250 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'priceCents'">
              {{ money(record.priceCents) }}
            </template>
            <template v-else-if="column.key === 'versionNo'">
              <Tag color="blue">v{{ record.versionNo }}</Tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === 1 ? 'green' : 'default'">
                {{ record.status === 1 ? '在售' : '历史/下架' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button size="small" type="link" @click="openVersionPlan(record)">
                  生成新版本
                </Button>
                <Button
                  v-if="record.status === 1"
                  danger
                  size="small"
                  type="link"
                  @click="disablePlan(record)"
                >
                  下架
                </Button>
              </Space>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="orders" tab="订单管理">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Input
            v-model:value="orderQuery.orderNo"
            allow-clear
            class="w-64"
            placeholder="订单号"
          />
          <Select
            v-model:value="orderQuery.status"
            :options="orderStatusOptions"
            allow-clear
            class="w-36"
            placeholder="订单状态"
          />
          <InputNumber
            v-model:value="orderQuery.memberId"
            class="w-40"
            placeholder="会员 ID"
          />
          <Button @click="searchOrders">查询</Button>
        </div>
        <Table
          :columns="orderColumns"
          :data-source="orders"
          :loading="ordersLoading"
          :pagination="orderPagination"
          :scroll="{ x: 1200 }"
          row-key="id"
          size="middle"
          @change="onOrderTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amountCents'">
              {{ money(record.amountCents) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDate(record.createdAt) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button size="small" type="link" @click="openDetail(record)">详情</Button>
                <Button
                  v-if="record.status === 'PAID'"
                  size="small"
                  type="link"
                  @click="openRefund(record)"
                >
                  退款审核
                </Button>
              </Space>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="payments" tab="支付流水">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Input
            v-model:value="paymentQuery.txnNo"
            allow-clear
            class="w-64"
            placeholder="交易号"
          />
          <Input
            v-model:value="paymentQuery.orderNo"
            allow-clear
            class="w-64"
            placeholder="订单号"
          />
          <Select
            v-model:value="paymentQuery.channel"
            :options="[
              { label: 'MOCK', value: 'MOCK' },
              { label: 'WECHAT_H5', value: 'WECHAT_H5' },
            ]"
            allow-clear
            class="w-40"
            placeholder="渠道"
          />
          <Button @click="searchPayments">查询</Button>
        </div>
        <Table
          :columns="paymentColumns"
          :data-source="payments"
          :loading="paymentsLoading"
          :pagination="paymentPagination"
          :scroll="{ x: 1100 }"
          row-key="id"
          size="middle"
          @change="onPaymentTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amountCents'">
              {{ money(record.amountCents) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
            </template>
            <template v-else-if="column.key === 'paidAt'">
              {{ formatDate(record.paidAt) }}
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>

    <Modal
      v-model:open="planOpen"
      :confirm-loading="planSaving"
      :title="editingPlan ? '生成套餐新版本' : '新增套餐'"
      width="560px"
      @ok="savePlan"
    >
      <div class="grid gap-3 py-2">
        <Input
          v-model:value="planForm.code"
          :disabled="Boolean(editingPlan)"
          addon-before="编码"
          placeholder="如 PREMIUM_30"
        />
        <Input v-model:value="planForm.name" addon-before="名称" />
        <div class="flex flex-wrap items-center gap-3">
          <InputNumber
            v-model:value="planForm.priceYuan"
            :min="0"
            :precision="2"
            addon-before="价格(元)"
            class="w-48"
          />
          <InputNumber
            v-model:value="planForm.durationDays"
            :min="1"
            addon-before="天数"
            class="w-40"
          />
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <InputNumber
            v-model:value="planForm.aiImageQuota"
            :min="0"
            addon-before="图片"
            class="w-40"
          />
          <InputNumber
            v-model:value="planForm.aiVideoQuota"
            :min="0"
            addon-before="视频"
            class="w-40"
          />
          <InputNumber
            v-model:value="planForm.benefitQuota"
            :min="0"
            addon-before="权益"
            class="w-40"
          />
        </div>
        <span class="text-xs text-gray-500">
          保存后旧版本自动下架，已售订单继续引用旧版本价格与额度。
        </span>
      </div>
    </Modal>

    <Modal
      v-model:open="detailOpen"
      :footer="null"
      :title="`订单详情 ${detail?.order.orderNo ?? ''}`"
      width="880px"
    >
      <div v-if="detailLoading" class="py-10 text-center text-gray-500">加载中…</div>
      <template v-else-if="detail">
        <Descriptions bordered :column="2" size="small">
          <DescriptionsItem label="订单状态">
            <Tag :color="statusColor(detail.order.status)">{{ detail.order.status }}</Tag>
          </DescriptionsItem>
          <DescriptionsItem label="实付">
            {{ money(detail.order.amountCents) }}
          </DescriptionsItem>
          <DescriptionsItem label="会员">
            {{ detail.order.memberNo }} / {{ detail.order.memberName }}
          </DescriptionsItem>
          <DescriptionsItem label="手机号">{{ detail.order.phone }}</DescriptionsItem>
          <DescriptionsItem label="套餐">
            {{ detail.order.planName }}（{{ detail.order.planCode }} v{{ detail.order.planVersionNo }}）
          </DescriptionsItem>
          <DescriptionsItem label="额度">
            图片 {{ detail.order.aiImageQuota }} / 视频 {{ detail.order.aiVideoQuota }} /
            权益 {{ detail.order.benefitQuota }}
          </DescriptionsItem>
          <DescriptionsItem label="创建时间">
            {{ formatDate(detail.order.createdAt) }}
          </DescriptionsItem>
          <DescriptionsItem label="支付时间">
            {{ formatDate(detail.order.paidAt) }}
          </DescriptionsItem>
          <DescriptionsItem label="周期">
            <template v-if="detail.order.periodId">
              {{ detail.order.periodNo }}
              （{{ detail.order.periodStatus }}）
            </template>
            <template v-else>-</template>
          </DescriptionsItem>
          <DescriptionsItem label="周期起止">
            {{ formatDate(detail.order.periodStartAt) }} ~
            {{ formatDate(detail.order.periodEndAt) }}
          </DescriptionsItem>
        </Descriptions>
        <Divider orientation="left">支付流水</Divider>
        <Table
          :columns="detailColumns"
          :data-source="detail.payments"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amountCents'">
              {{ money(record.amountCents) }}
            </template>
            <template v-else-if="column.key === 'paidAt'">
              {{ formatDate(record.paidAt) }}
            </template>
          </template>
        </Table>
        <Divider orientation="left">退款记录</Divider>
        <Table
          :columns="refundColumns"
          :data-source="detail.refunds"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amountCents'">
              {{ money(record.amountCents) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusColor(record.status)">{{ record.status }}</Tag>
            </template>
            <template v-else-if="column.key === 'processedAt'">
              {{ formatDate(record.processedAt) }}
            </template>
          </template>
        </Table>
      </template>
    </Modal>

    <Modal
      v-model:open="refundOpen"
      :confirm-loading="refundSaving"
      title="退款审核"
      width="520px"
      @ok="submitRefund"
    >
      <div class="grid gap-3 py-2">
        <div class="text-xs text-gray-500">
          订单 {{ refundTarget?.orderNo }}，实付 {{ money(refundTarget?.amountCents ?? 0) }}。
          通过后立即终止该订单对应周期并回收未用名额，已发出的权益不回滚。
        </div>
        <Radio.Group v-model:value="refundForm.result" button-style="solid">
          <Radio.Button value="APPROVE">通过退款</Radio.Button>
          <Radio.Button value="REJECT">驳回</Radio.Button>
        </Radio.Group>
        <InputNumber
          v-model:value="refundForm.amountYuan"
          :max="(refundTarget?.amountCents ?? 0) / 100"
          :min="0.01"
          :precision="2"
          addon-before="退款(元)"
          class="w-full"
        />
        <Input.TextArea
          v-model:value="refundForm.reason"
          :rows="3"
          placeholder="退款原因（必填，写入审计）"
        />
      </div>
    </Modal>
  </div>
</template>
