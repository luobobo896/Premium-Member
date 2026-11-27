<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { SubscriptionApi } from '#/api';

import { onMounted, reactive, ref } from 'vue';

import {
  Button,
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
import dayjs from 'dayjs';

import {
  getSubscriptionChargesApi,
  getSubscriptionsApi,
  grantSubscriptionPointsApi,
  runSubscriptionBillingApi,
  runSubscriptionDunningApi,
} from '#/api';

const TabPane = Tabs.TabPane;

const activeTab = ref('subscriptions');
const subscriptions = ref<SubscriptionApi.SubscriptionRow[]>([]);
const subscriptionLoading = ref(false);
const subscriptionQuery = reactive({
  memberNo: '',
  page: 1,
  pageSize: 10,
  status: '',
});
const subscriptionPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

const charges = ref<SubscriptionApi.ChargeRow[]>([]);
const chargeLoading = ref(false);
const chargeQuery = reactive({
  memberNo: '',
  page: 1,
  pageSize: 10,
  period: '',
  status: '',
});
const chargePagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

const grantOpen = ref(false);
const grantSaving = ref(false);
const grantTarget = ref<SubscriptionApi.SubscriptionRow | null>(null);
const grantForm = reactive({ points: 1000, reason: '' });

const runOpen = ref(false);
const runSaving = ref(false);
const runForm = reactive({
  graceDays: 3,
  mockResult: 'SUCCESS',
  period: '',
  subscriptionId: undefined as number | undefined,
});

const statusOptions = [
  { label: '正常', value: 'ACTIVE' },
  { label: '待扣款', value: 'PAST_DUE' },
  { label: '已停用', value: 'SUSPENDED' },
  { label: '已取消', value: 'CANCELLED' },
];

const chargeStatusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '处理中', value: 'PENDING' },
];

const subscriptionColumns: TableColumnsType = [
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员编号', width: 150 },
  { dataIndex: 'memberName', key: 'memberName', title: '会员', width: 120 },
  { dataIndex: 'phone', key: 'phone', title: '手机号', width: 130 },
  { dataIndex: 'planName', key: 'planName', title: '套餐', minWidth: 150 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'points', key: 'points', title: '积分', width: 120 },
  { dataIndex: 'nextChargeAt', key: 'nextChargeAt', title: '下次扣款', width: 160 },
  { dataIndex: 'lastChargeStatus', key: 'lastChargeStatus', title: '最近扣款', width: 150 },
  { dataIndex: 'graceUntil', key: 'graceUntil', title: '宽限至', width: 160 },
  { key: 'action', title: '操作', width: 190, fixed: 'right' },
];

const chargeColumns: TableColumnsType = [
  { dataIndex: 'chargeNo', key: 'chargeNo', title: '代扣单号', width: 240 },
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员编号', width: 150 },
  { dataIndex: 'memberName', key: 'memberName', title: '会员', width: 120 },
  { dataIndex: 'period', key: 'period', title: '账期', width: 90 },
  { dataIndex: 'amountCents', key: 'amountCents', title: '金额', width: 100 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'attemptNo', key: 'attemptNo', title: '尝试', width: 70 },
  { dataIndex: 'provider', key: 'provider', title: '渠道', width: 90 },
  { dataIndex: 'failReason', key: 'failReason', title: '失败原因', minWidth: 180 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '创建时间', width: 160 },
];

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function money(cents: number) {
  return `¥${(cents / 100).toFixed(2)}`;
}

function subscriptionStatusColor(status: string) {
  if (status === 'ACTIVE') return 'green';
  if (status === 'PAST_DUE') return 'orange';
  if (status === 'SUSPENDED') return 'red';
  return 'default';
}

function chargeStatusColor(status: string) {
  if (status === 'SUCCESS') return 'green';
  if (status === 'FAILED') return 'red';
  return 'blue';
}

async function loadSubscriptions() {
  subscriptionLoading.value = true;
  try {
    const result = await getSubscriptionsApi({
      memberNo: subscriptionQuery.memberNo || undefined,
      page: subscriptionQuery.page,
      pageSize: subscriptionQuery.pageSize,
      status: subscriptionQuery.status || undefined,
    });
    subscriptions.value = result.items;
    subscriptionPagination.total = result.total;
  } finally {
    subscriptionLoading.value = false;
  }
}

async function loadCharges() {
  chargeLoading.value = true;
  try {
    const result = await getSubscriptionChargesApi({
      memberNo: chargeQuery.memberNo || undefined,
      page: chargeQuery.page,
      pageSize: chargeQuery.pageSize,
      period: chargeQuery.period || undefined,
      status: chargeQuery.status || undefined,
    });
    charges.value = result.items;
    chargePagination.total = result.total;
  } finally {
    chargeLoading.value = false;
  }
}

function searchSubscriptions() {
  subscriptionQuery.page = 1;
  loadSubscriptions();
}

function searchCharges() {
  chargeQuery.page = 1;
  loadCharges();
}

function onSubscriptionTableChange(page: any) {
  subscriptionQuery.page = page.current;
  subscriptionQuery.pageSize = page.pageSize;
  loadSubscriptions();
}

function onChargeTableChange(page: any) {
  chargeQuery.page = page.current;
  chargeQuery.pageSize = page.pageSize;
  loadCharges();
}

function openGrant(record: SubscriptionApi.SubscriptionRow) {
  grantTarget.value = record;
  grantForm.points = 1000;
  grantForm.reason = '';
  grantOpen.value = true;
}

async function submitGrant() {
  if (!grantTarget.value) return;
  if (!grantForm.reason.trim()) {
    message.warning('请填写补发原因');
    return;
  }
  grantSaving.value = true;
  try {
    await grantSubscriptionPointsApi(grantTarget.value.id, {
      points: grantForm.points,
      reason: grantForm.reason.trim(),
    });
    message.success('积分已补发，已写入账本和操作审计');
    grantOpen.value = false;
    await loadSubscriptions();
  } finally {
    grantSaving.value = false;
  }
}

function openRun(subscriptionId?: number) {
  runForm.subscriptionId = subscriptionId;
  runForm.mockResult = 'SUCCESS';
  runForm.graceDays = 3;
  runForm.period = '';
  runOpen.value = true;
}

async function submitRun() {
  runSaving.value = true;
  try {
    const result = await runSubscriptionBillingApi({
      graceDays: runForm.graceDays,
      mockResult: runForm.mockResult,
      period: runForm.period || undefined,
      subscriptionId: runForm.subscriptionId,
    });
    message.success(
      `执行完成：处理 ${result.processed}，发放 ${result.granted}，失败 ${result.failed}，停用 ${result.suspended}`,
    );
    runOpen.value = false;
    await Promise.all([loadSubscriptions(), loadCharges()]);
  } finally {
    runSaving.value = false;
  }
}

async function runDunning(record: SubscriptionApi.SubscriptionRow) {
  Modal.confirm({
    content: `对 ${record.memberNo} 执行宽限重试？宽限已过会直接停用。`,
    onOk: async () => {
      const result = await runSubscriptionDunningApi({
        forceSuspend: false,
        subscriptionId: record.id,
      });
      message.success(`重试完成：失败 ${result.failed}，停用 ${result.suspended}`);
      await Promise.all([loadSubscriptions(), loadCharges()]);
    },
    title: '执行宽限重试',
  });
}

onMounted(() => {
  loadSubscriptions();
  loadCharges();
});
</script>

<template>
  <div class="p-4">
    <Tabs v-model:active-key="activeTab">
      <TabPane key="subscriptions" tab="订阅列表">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Input
            v-model:value="subscriptionQuery.memberNo"
            allow-clear
            placeholder="会员编号"
            style="width: 180px"
            @press-enter="searchSubscriptions"
          />
          <Select
            v-model:value="subscriptionQuery.status"
            :options="statusOptions"
            allow-clear
            placeholder="订阅状态"
            style="width: 150px"
          />
          <Button @click="searchSubscriptions">查询</Button>
          <div class="flex-1"></div>
          <Button type="primary" @click="openRun()">执行 1 号任务</Button>
        </div>
        <Table
          :columns="subscriptionColumns"
          :data-source="subscriptions"
          :loading="subscriptionLoading"
          :pagination="subscriptionPagination"
          :scroll="{ x: 1500 }"
          row-key="id"
          size="middle"
          @change="onSubscriptionTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <Tag :color="subscriptionStatusColor(record.status)">
                {{ record.status }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'points'">
              {{ record.balancePoints }} / {{ record.monthlyPoints }}
            </template>
            <template v-else-if="column.key === 'nextChargeAt'">
              {{ formatDate(record.nextChargeAt) }}
            </template>
            <template v-else-if="column.key === 'lastChargeStatus'">
              <Tag
                v-if="record.lastChargeStatus"
                :color="chargeStatusColor(record.lastChargeStatus)"
              >
                {{ record.lastChargeStatus }}
              </Tag>
              <span v-else>-</span>
              <div v-if="record.lastChargeReason" class="text-xs text-gray-500">
                {{ record.lastChargeReason }}
              </div>
            </template>
            <template v-else-if="column.key === 'graceUntil'">
              {{ formatDate(record.graceUntil) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button size="small" type="link" @click="openGrant(record)">补发积分</Button>
                <Button
                  v-if="record.status === 'ACTIVE'"
                  size="small"
                  type="link"
                  @click="openRun(record.id)"
                >
                  模拟扣款
                </Button>
                <Button
                  v-if="record.status === 'PAST_DUE'"
                  size="small"
                  type="link"
                  @click="runDunning(record)"
                >
                  宽限重试
                </Button>
              </Space>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="charges" tab="代扣记录">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Input
            v-model:value="chargeQuery.memberNo"
            allow-clear
            placeholder="会员编号"
            style="width: 180px"
            @press-enter="searchCharges"
          />
          <Input
            v-model:value="chargeQuery.period"
            allow-clear
            placeholder="账期 YYYY-MM"
            style="width: 150px"
            @press-enter="searchCharges"
          />
          <Select
            v-model:value="chargeQuery.status"
            :options="chargeStatusOptions"
            allow-clear
            placeholder="代扣状态"
            style="width: 140px"
          />
          <Button @click="searchCharges">查询</Button>
        </div>
        <Table
          :columns="chargeColumns"
          :data-source="charges"
          :loading="chargeLoading"
          :pagination="chargePagination"
          :scroll="{ x: 1500 }"
          row-key="id"
          size="middle"
          @change="onChargeTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amountCents'">
              {{ money(record.amountCents) }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="chargeStatusColor(record.status)">{{ record.status }}</Tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDate(record.createdAt) }}
            </template>
          </template>
        </Table>
      </TabPane>
    </Tabs>

    <Modal
      v-model:open="grantOpen"
      :confirm-loading="grantSaving"
      title="手动补发积分"
      @ok="submitGrant"
    >
      <div class="grid gap-3 py-2">
        <span class="text-sm text-gray-500">
          {{ grantTarget?.memberNo }} · {{ grantTarget?.memberName }}
        </span>
        <InputNumber v-model:value="grantForm.points" :min="1" addon-before="积分" />
        <Input.TextArea
          v-model:value="grantForm.reason"
          :rows="3"
          placeholder="补发原因（必填，写入操作审计）"
        />
      </div>
    </Modal>

    <Modal
      v-model:open="runOpen"
      :confirm-loading="runSaving"
      title="执行订阅任务"
      @ok="submitRun"
    >
      <div class="grid gap-3 py-2">
        <InputNumber
          v-model:value="runForm.subscriptionId"
          :min="1"
          addon-before="订阅 ID"
          placeholder="留空则处理所有到期订阅"
        />
        <Input v-model:value="runForm.period" placeholder="账期 YYYY-MM（留空按下次扣款日）" />
        <Select
          v-model:value="runForm.mockResult"
          :options="[
            { label: 'Mock 成功', value: 'SUCCESS' },
            { label: 'Mock 失败', value: 'FAIL' },
          ]"
        />
        <InputNumber v-model:value="runForm.graceDays" :min="0" addon-before="宽限天数" />
        <span class="text-xs text-gray-500">
          清零、代扣、发放积分均按账期幂等；重复执行不会重复发放。
        </span>
      </div>
    </Modal>
  </div>
</template>
