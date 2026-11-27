import { expect, test } from '@playwright/test';

const C_API = process.env.E2E_C_API_BASE ?? 'http://127.0.0.1:8081';
const ADMIN_API = process.env.E2E_ADMIN_API_BASE ?? 'http://127.0.0.1:5320/api';
const PASSWORD = 'E2E123456';

let phone = '';

test.beforeAll(async ({ request }) => {
  const suffix = String(Date.now()).slice(-9);
  phone = `13${suffix}`;

  const adminLogin = await request.post(`${ADMIN_API}/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456' },
  });
  expect(adminLogin.ok(), await adminLogin.text()).toBeTruthy();
  const adminBody = await adminLogin.json();
  const adminToken = adminBody.data.accessToken as string;

  const member = await request.post(`${ADMIN_API}/members`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: {
      username: `e2e_${suffix}`,
      nickname: 'E2E 会员',
      phone,
      level: 'BASIC',
      points: 0,
      status: 1,
      remark: 'playwright e2e',
    },
  });
  expect(member.ok(), await member.text()).toBeTruthy();

  const register = await request.post(`${C_API}/c/auth/register`, {
    data: { phone, password: PASSWORD, nickname: 'E2E 会员' },
  });
  expect(register.ok(), await register.text()).toBeTruthy();
});

test('guest browses catalog, logs in, pays and sees paid order', async ({ page }) => {
  await page.goto('./');
  await expect(page.getByRole('heading', { name: 'AI 创作' })).toBeVisible();

  await page.getByRole('button', { name: '浏览权益目录' }).click();
  await expect(page.getByRole('heading', { name: '权益目录' })).toBeVisible();
  await expect(page.getByText('咖啡兑换券')).toBeVisible();

  await page.getByRole('button', { name: '首页' }).click();
  await page.getByText('查看套餐').click();
  await expect(page.getByRole('heading', { name: '选择购买' })).toBeVisible();
  await expect(page.getByText('AI 创作会员 · 30 天')).toBeVisible();

  await page.getByText('我已阅读并同意《会员服务协议》').click();
  await page.getByRole('button', { name: '登录后购买' }).click();
  await expect(page.getByRole('heading', { name: '登录会员账号' })).toBeVisible();

  await page.getByPlaceholder('请输入 11 位手机号').fill(phone);
  await page.getByPlaceholder('请输入密码').fill(PASSWORD);
  await page.getByRole('button', { name: '登录', exact: true }).click();

  await expect(page.getByRole('heading', { name: '支付成功' })).toBeVisible({ timeout: 30_000 });
  await expect(page.getByText('已开通')).toBeVisible();

  await page.getByRole('button', { name: '查看我的订单' }).click();
  await expect(page.getByRole('heading', { name: '购买记录' })).toBeVisible();
  await expect(page.getByText('已支付').first()).toBeVisible();
});
