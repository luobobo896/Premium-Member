-- Development seed data. Passwords are development-only and must never be reused in production.

INSERT INTO sys_dept (id, parent_id, name, leader, phone, sort, status) VALUES
    (1, 0, '总公司', 'Admin', '13800000001', 1, 1),
    (2, 1, '运营部', 'Operator', '13800000002', 2, 1),
    (3, 1, '技术部', 'Viewer', '13800000003', 3, 1),
    (6, 2, '运营二组', 'GroupUser', '13800000006', 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, code, name, data_scope, status, sort, remark) VALUES
    (1, 'super_admin', '超级管理员', 'ALL', 1, 1, '拥有全部菜单和权限'),
    (2, 'operator', '运营人员', 'DEPT', 1, 2, '会员日常运营'),
    (3, 'viewer', '只读人员', 'SELF', 1, 3, '只读查询'),
    (4, 'dept_manager', '部门管理员', 'DEPT', 1, 4, '仅本部门会员与用户'),
    (5, 'self_manager', '个人管理员', 'SELF', 1, 5, '仅本人数据'),
    (6, 'dept_and_child', '部门及下级管理员', 'DEPT_AND_CHILD', 1, 6, '本部门及下级部门')
ON CONFLICT (id) DO NOTHING;

UPDATE sys_role SET data_scope = 'ALL' WHERE code = 'super_admin' AND data_scope <> 'ALL';
UPDATE sys_role SET data_scope = 'DEPT' WHERE code = 'operator' AND data_scope <> 'DEPT';
UPDATE sys_role SET data_scope = 'SELF' WHERE code = 'viewer' AND data_scope <> 'SELF';
UPDATE sys_role SET data_scope = 'DEPT_AND_CHILD'
WHERE code = 'dept_and_child' AND data_scope <> 'DEPT_AND_CHILD';

INSERT INTO sys_permission (id, code, name, type, description) VALUES
    (1, 'dashboard:view', '查看仪表盘', 'API', '访问仪表盘页面'),
    (2, 'system:user:list', '用户列表', 'API', '查询用户'),
    (3, 'system:user:create', '创建用户', 'BUTTON', '创建用户'),
    (4, 'system:user:update', '编辑用户', 'BUTTON', '编辑用户'),
    (5, 'system:user:delete', '删除用户', 'BUTTON', '删除用户'),
    (6, 'system:role:list', '角色列表', 'API', '查询角色'),
    (7, 'system:role:create', '创建角色', 'BUTTON', '创建角色'),
    (8, 'system:role:update', '编辑角色', 'BUTTON', '编辑角色'),
    (9, 'system:role:delete', '删除角色', 'BUTTON', '删除角色'),
    (10, 'system:menu:list', '菜单列表', 'API', '查询菜单'),
    (11, 'system:menu:create', '创建菜单', 'BUTTON', '创建菜单'),
    (12, 'system:menu:update', '编辑菜单', 'BUTTON', '编辑菜单'),
    (13, 'system:menu:delete', '删除菜单', 'BUTTON', '删除菜单'),
    (14, 'system:dept:list', '部门列表', 'API', '查询部门'),
    (15, 'system:dept:create', '创建部门', 'BUTTON', '创建部门'),
    (16, 'system:dept:update', '编辑部门', 'BUTTON', '编辑部门'),
    (17, 'system:dept:delete', '删除部门', 'BUTTON', '删除部门'),
    (18, 'member:list', '会员列表', 'API', '分页查询会员'),
    (19, 'member:read', '会员详情', 'API', '查看会员详情'),
    (20, 'member:create', '创建会员', 'BUTTON', '创建会员'),
    (21, 'member:update', '编辑会员', 'BUTTON', '编辑会员'),
    (22, 'member:delete', '删除会员', 'BUTTON', '删除会员'),
    (23, 'system:log:list', '操作日志', 'API', '查询操作日志'),
    (24, 'benefit:product:list', '权益商品列表', 'API', '查询权益商品与库存'),
    (25, 'benefit:product:create', '新增权益商品', 'BUTTON', '新增权益商品'),
    (26, 'benefit:product:update', '编辑权益商品', 'BUTTON', '编辑权益商品'),
    (27, 'benefit:product:delete', '删除权益商品', 'BUTTON', '删除或下架权益商品'),
    (28, 'benefit:card:list', '卡密列表', 'API', '查询卡密（脱敏）'),
    (29, 'benefit:card:import', '导入卡密', 'BUTTON', '批量导入卡密'),
    (30, 'benefit:claim:list', '领取单列表', 'API', '查询权益领取单'),
    (31, 'benefit:claim:review', '人工核实', 'BUTTON', '人工核实、补偿或撤销领取单'),
    (32, 'plan:list', '套餐列表', 'API', '查询套餐版本'),
    (33, 'plan:create', '新增套餐', 'BUTTON', '新增套餐或生成新版本'),
    (34, 'plan:update', '编辑套餐', 'BUTTON', '修改套餐并生成新版本'),
    (35, 'plan:delete', '下架套餐', 'BUTTON', '下架当前在售套餐'),
    (36, 'order:list', '订单列表', 'API', '分页查询订单与详情'),
    (37, 'order:refund', '退款审核', 'BUTTON', '审核整单退款'),
    (38, 'payment:list', '支付流水', 'API', '查询支付交易流水'),
    (39, 'ai:template:list', 'AI 模板列表', 'API', '查询 AI 模板'),
    (40, 'ai:template:create', '新增 AI 模板', 'BUTTON', '新增 AI 模板'),
    (41, 'ai:template:update', '编辑 AI 模板', 'BUTTON', '编辑或上下架 AI 模板'),
    (42, 'ai:template:delete', '删除 AI 模板', 'BUTTON', '删除 AI 模板'),
    (43, 'ai:task:list', 'AI 任务查询', 'API', '分页查询 AI 任务'),
    (44, 'ai:usage:list', 'AI 用量日报', 'API', '查询 AI 每日用量'),
    (45, 'ticket:list', '工单列表', 'API', '分页查询客服工单'),
    (46, 'ticket:handle', '工单处理', 'BUTTON', '受理、回复、改状态与备注'),
    (47, 'content:list', '内容配置列表', 'API', '查询内容配置与版本历史'),
    (48, 'content:update', '编辑内容草稿', 'BUTTON', '新增或编辑内容草稿'),
    (49, 'content:publish', '发布内容', 'BUTTON', '发布内容草稿'),
    (50, 'content:rollback', '回滚内容', 'BUTTON', '回滚到上一已发布版本'),
    (51, 'stats:daily', '统计日报', 'API', '查询每日运营统计'),
    (52, 'stats:export', '统计导出', 'BUTTON', '导出统计 CSV 并记录审计')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, name, path, component, redirect, type, title, icon,
                      permission_code, sort, status, visible, keep_alive, affix_tab, hide_in_menu) VALUES
    (1, 0, 'Dashboard', '/dashboard', NULL, '/analytics', 'catalog', '仪表盘',
     'lucide:layout-dashboard', 'dashboard:view', -1, 1, 1, 0, 0, 0),
    (2, 1, 'Analytics', '/analytics', '/dashboard/analytics/index', NULL, 'menu', '分析页',
     'lucide:area-chart', 'dashboard:view', 1, 1, 1, 0, 1, 0),
    (3, 1, 'Workspace', '/workspace', '/dashboard/workspace/index', NULL, 'menu', '工作台',
     'carbon:workspace', 'dashboard:view', 2, 1, 1, 0, 0, 0),
    (10, 0, 'Member', '/member', NULL, '/members', 'catalog', '会员管理',
     'lucide:users', NULL, 10, 1, 1, 0, 0, 0),
    (11, 10, 'MemberList', '/members', '/member/list/index', NULL, 'menu', '会员列表',
     'lucide:contact', 'member:list', 1, 1, 1, 0, 0, 0),
    (12, 11, 'MemberCreate', '', NULL, NULL, 'button', '新增会员',
     NULL, 'member:create', 1, 1, 1, 0, 0, 0),
    (13, 11, 'MemberUpdate', '', NULL, NULL, 'button', '编辑会员',
     NULL, 'member:update', 2, 1, 1, 0, 0, 0),
    (14, 11, 'MemberDelete', '', NULL, NULL, 'button', '删除会员',
     NULL, 'member:delete', 3, 1, 1, 0, 0, 0),
    (15, 11, 'MemberView', '', NULL, NULL, 'button', '查看会员',
     NULL, 'member:read', 4, 1, 1, 0, 0, 0),
    (20, 0, 'System', '/system', NULL, '/system/menu', 'catalog', '系统管理',
     'carbon:settings', NULL, 20, 1, 1, 0, 0, 0),
    (21, 20, 'SystemMenu', '/system/menu', '/system/menu/index', NULL, 'menu', '菜单管理',
     'carbon:menu', 'system:menu:list', 1, 1, 1, 0, 0, 0),
    (211, 21, 'SystemMenuCreate', '', NULL, NULL, 'button', '新增菜单',
     NULL, 'system:menu:create', 1, 1, 1, 0, 0, 0),
    (212, 21, 'SystemMenuUpdate', '', NULL, NULL, 'button', '编辑菜单',
     NULL, 'system:menu:update', 2, 1, 1, 0, 0, 0),
    (213, 21, 'SystemMenuDelete', '', NULL, NULL, 'button', '删除菜单',
     NULL, 'system:menu:delete', 3, 1, 1, 0, 0, 0),
    (22, 20, 'SystemDept', '/system/dept', '/system/dept/index', NULL, 'menu', '部门管理',
     'carbon:container-services', 'system:dept:list', 2, 1, 1, 0, 0, 0),
    (221, 22, 'SystemDeptCreate', '', NULL, NULL, 'button', '新增部门',
     NULL, 'system:dept:create', 1, 1, 1, 0, 0, 0),
    (222, 22, 'SystemDeptUpdate', '', NULL, NULL, 'button', '编辑部门',
     NULL, 'system:dept:update', 2, 1, 1, 0, 0, 0),
    (223, 22, 'SystemDeptDelete', '', NULL, NULL, 'button', '删除部门',
     NULL, 'system:dept:delete', 3, 1, 1, 0, 0, 0),
    (23, 20, 'SystemRole', '/system/role', '/system/role/index', NULL, 'menu', '角色管理',
     'carbon:user-role', 'system:role:list', 3, 1, 1, 0, 0, 0),
    (231, 23, 'SystemRoleCreate', '', NULL, NULL, 'button', '新增角色',
     NULL, 'system:role:create', 1, 1, 1, 0, 0, 0),
    (232, 23, 'SystemRoleUpdate', '', NULL, NULL, 'button', '编辑角色',
     NULL, 'system:role:update', 2, 1, 1, 0, 0, 0),
    (233, 23, 'SystemRoleDelete', '', NULL, NULL, 'button', '删除角色',
     NULL, 'system:role:delete', 3, 1, 1, 0, 0, 0),
    (24, 20, 'SystemUser', '/system/user', '/system/user/index', NULL, 'menu', '用户管理',
     'carbon:user', 'system:user:list', 4, 1, 1, 0, 0, 0),
    (241, 24, 'SystemUserCreate', '', NULL, NULL, 'button', '新增用户',
     NULL, 'system:user:create', 1, 1, 1, 0, 0, 0),
    (242, 24, 'SystemUserUpdate', '', NULL, NULL, 'button', '编辑用户',
     NULL, 'system:user:update', 2, 1, 1, 0, 0, 0),
    (243, 24, 'SystemUserDelete', '', NULL, NULL, 'button', '删除用户',
     NULL, 'system:user:delete', 3, 1, 1, 0, 0, 0),
    (25, 20, 'SystemLog', '/system/log', '/system/log/index', NULL, 'menu', '操作日志',
     'carbon:document', 'system:log:list', 5, 1, 1, 0, 0, 0),
    (30, 0, 'About', '/about', '/_core/about/index', NULL, 'menu', '关于',
     'lucide:copyright', NULL, 9999, 1, 1, 0, 0, 0),
    (40, 0, 'Benefit', '/benefit', '/benefit/index', NULL, 'menu', '权益管理',
     'lucide:gift', 'benefit:product:list', 15, 1, 1, 0, 0, 0),
    (401, 40, 'BenefitProductCreate', '', NULL, NULL, 'button', '新增权益商品',
     NULL, 'benefit:product:create', 1, 1, 1, 0, 0, 0),
    (402, 40, 'BenefitProductUpdate', '', NULL, NULL, 'button', '编辑权益商品',
     NULL, 'benefit:product:update', 2, 1, 1, 0, 0, 0),
    (403, 40, 'BenefitProductDelete', '', NULL, NULL, 'button', '删除权益商品',
     NULL, 'benefit:product:delete', 3, 1, 1, 0, 0, 0),
    (404, 40, 'BenefitCardList', '', NULL, NULL, 'button', '查看卡密',
     NULL, 'benefit:card:list', 4, 1, 1, 0, 0, 0),
    (405, 40, 'BenefitCardImport', '', NULL, NULL, 'button', '导入卡密',
     NULL, 'benefit:card:import', 5, 1, 1, 0, 0, 0),
    (406, 40, 'BenefitClaimList', '', NULL, NULL, 'button', '查看领取单',
     NULL, 'benefit:claim:list', 6, 1, 1, 0, 0, 0),
    (407, 40, 'BenefitClaimReview', '', NULL, NULL, 'button', '人工核实领取单',
     NULL, 'benefit:claim:review', 7, 1, 1, 0, 0, 0),
    (50, 0, 'Trade', '/trade', '/trade/index', NULL, 'menu', '交易管理',
     'lucide:receipt', 'plan:list', 16, 1, 1, 0, 0, 0),
    (501, 50, 'PlanCreate', '', NULL, NULL, 'button', '新增套餐/版本',
     NULL, 'plan:create', 1, 1, 1, 0, 0, 0),
    (502, 50, 'PlanUpdate', '', NULL, NULL, 'button', '编辑套餐',
     NULL, 'plan:update', 2, 1, 1, 0, 0, 0),
    (503, 50, 'PlanDelete', '', NULL, NULL, 'button', '下架套餐',
     NULL, 'plan:delete', 3, 1, 1, 0, 0, 0),
    (504, 50, 'OrderList', '', NULL, NULL, 'button', '查看订单',
     NULL, 'order:list', 4, 1, 1, 0, 0, 0),
    (505, 50, 'OrderRefund', '', NULL, NULL, 'button', '退款审核',
     NULL, 'order:refund', 5, 1, 1, 0, 0, 0),
    (506, 50, 'PaymentList', '', NULL, NULL, 'button', '查看支付流水',
     NULL, 'payment:list', 6, 1, 1, 0, 0, 0),
    (60, 0, 'Ai', '/ai', '/ai/index', NULL, 'menu', 'AI 创作',
     'lucide:sparkles', 'ai:template:list', 17, 1, 1, 0, 0, 0),
    (601, 60, 'AiTemplateCreate', '', NULL, NULL, 'button', '新增 AI 模板',
     NULL, 'ai:template:create', 1, 1, 1, 0, 0, 0),
    (602, 60, 'AiTemplateUpdate', '', NULL, NULL, 'button', '编辑 AI 模板',
     NULL, 'ai:template:update', 2, 1, 1, 0, 0, 0),
    (603, 60, 'AiTemplateDelete', '', NULL, NULL, 'button', '删除 AI 模板',
     NULL, 'ai:template:delete', 3, 1, 1, 0, 0, 0),
    (604, 60, 'AiTaskList', '', NULL, NULL, 'button', 'AI 任务查询',
     NULL, 'ai:task:list', 4, 1, 1, 0, 0, 0),
    (605, 60, 'AiUsageList', '', NULL, NULL, 'button', 'AI 用量日报',
     NULL, 'ai:usage:list', 5, 1, 1, 0, 0, 0),
    (70, 0, 'Ticket', '/tickets', '/tickets/index', NULL, 'menu', '工单管理',
     'lucide:headphones', 'ticket:list', 18, 1, 1, 0, 0, 0),
    (701, 70, 'TicketHandle', '', NULL, NULL, 'button', '工单处理',
     NULL, 'ticket:handle', 1, 1, 1, 0, 0, 0),
    (80, 0, 'Content', '/contents', '/content/index', NULL, 'menu', '内容配置',
     'lucide:file-text', 'content:list', 19, 1, 1, 0, 0, 0),
    (801, 80, 'ContentUpdate', '', NULL, NULL, 'button', '编辑内容草稿',
     NULL, 'content:update', 1, 1, 1, 0, 0, 0),
    (802, 80, 'ContentPublish', '', NULL, NULL, 'button', '发布内容',
     NULL, 'content:publish', 2, 1, 1, 0, 0, 0),
    (803, 80, 'ContentRollback', '', NULL, NULL, 'button', '回滚内容',
     NULL, 'content:rollback', 3, 1, 1, 0, 0, 0),
    (90, 0, 'Stats', '/stats', '/stats/index', NULL, 'menu', '运营统计',
     'lucide:chart-column', 'stats:daily', 20, 1, 1, 0, 0, 0),
    (901, 90, 'StatsExport', '', NULL, NULL, 'button', '导出统计',
     NULL, 'stats:export', 1, 1, 1, 0, 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user (id, username, password_hash, real_name, avatar, description, home_path,
                      email, phone, dept_id, status) VALUES
    (1, 'admin', '$2y$10$t6mhUt3kCDCN7Pa20kgeBuUmuTk2n86KH5zCNANcfMfn14PJTMHfu',
     'Administrator', '', '超级管理员', '/analytics', 'admin@example.com', '13800000001', 1, 1),
    (2, 'operator', '$2y$10$boZDjsyIMXZHXDJ7QLHXfuuoTn/lXNuueI7kth300RJe/eqhFvKeq',
     'Operator', '', '运营人员', '/workspace', 'operator@example.com', '13800000002', 2, 1),
    (3, 'viewer', '$2y$10$cSoK3xHDGsR7MbIxakhYz.XKYT7m2sWe.kdTQBwz3h0bGrEwFLfky',
     'Viewer', '', '只读人员', '/analytics', 'viewer@example.com', '13800000003', 3, 1),
    (4, 'dept_manager', '$2y$10$rFKgXFX2M13lJ1RCg4KZJOrxRPQQdDZDlmQzWyl7HhYU1pD04qH8K',
     'Dept Manager', '', '部门数据权限验收账号', '/members', 'dept_manager@example.com', '13800000004', 2, 1),
    (5, 'self_manager', '$2y$10$rFKgXFX2M13lJ1RCg4KZJOrxRPQQdDZDlmQzWyl7HhYU1pD04qH8K',
     'Self Manager', '', '个人数据权限验收账号', '/members', 'self_manager@example.com', '13800000005', 2, 1),
    (6, 'dept_and_child', '$2y$10$rFKgXFX2M13lJ1RCg4KZJOrxRPQQdDZDlmQzWyl7HhYU1pD04qH8K',
     'Dept And Child', '', '本部门及下级部门验收账号', '/members', 'dept_and_child@example.com',
     '13800000006', 2, 1),
    (7, 'group_user', '$2y$10$rFKgXFX2M13lJ1RCg4KZJOrxRPQQdDZDlmQzWyl7HhYU1pD04qH8K',
     'Group User', '', '运营二组组员', '/members', 'group_user@example.com', '13800000007', 6, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 3)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE code IN (
    'dashboard:view', 'member:list', 'member:read', 'member:create', 'member:update',
    'system:dept:list'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE code IN ('dashboard:view', 'member:list', 'member:read')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE code IN (
    'dashboard:view', 'member:list', 'member:read', 'system:user:list'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 5, id FROM sys_permission WHERE code IN (
    'dashboard:view', 'member:list', 'member:read', 'system:user:list'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 6, id FROM sys_permission WHERE code IN (
    'dashboard:view', 'member:list', 'member:read', 'system:user:list'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1, 2, 3, 10, 11, 12, 13, 15, 30)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE id IN (1, 2, 3, 10, 11, 15, 30)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 4, id FROM sys_menu WHERE id IN (1, 2, 3, 10, 11, 15, 20, 24)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 5, id FROM sys_menu WHERE id IN (1, 2, 3, 10, 11, 15, 20, 24)
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 6, id FROM sys_menu WHERE id IN (1, 2, 3, 10, 11, 15, 20, 24)
ON CONFLICT DO NOTHING;

INSERT INTO pm_member (id, member_no, username, owner_id, nickname, phone, level, points, balance, status,
                       joined_at, remark) VALUES
    (1, 'M202611270001', 'member001', 1, '张三', '13900000001', 'GOLD', 1200, 88.50, 1,
     now() - INTERVAL '180 days', '种子会员'),
    (2, 'M202611270002', 'member002', 2, '李四', '13900000002', 'SILVER', 320, 0.00, 1,
     now() - INTERVAL '60 days', '种子会员'),
    (3, 'M202611270003', 'member003', 3, '王五', '13900000003', 'BASIC', 0, 0.00, 0,
     now() - INTERVAL '10 days', '已停用示例'),
    (4, 'M202611270004', 'member004', 4, '部门会员', '13900000004', 'SILVER', 100, 0.00, 1,
     now() - INTERVAL '5 days', 'DEPT 验收样例'),
    (5, 'M202611270005', 'member005', 5, '个人会员', '13900000005', 'BASIC', 10, 0.00, 1,
     now() - INTERVAL '2 days', 'SELF 验收样例'),
    (6, 'M202611270006', 'member006', 7, '组员会员', '13900000006', 'BASIC', 5, 0.00, 1,
     now() - INTERVAL '1 days', 'DEPT_AND_CHILD 验收样例')
ON CONFLICT (id) DO NOTHING;

UPDATE pm_member SET owner_id = id WHERE owner_id IS NULL AND id IN (1, 2, 3, 4, 5, 6);

INSERT INTO pm_member_period (id, member_id, period_no, start_at, end_at, status, order_id) VALUES
    (1, 1, '2026-11', now() - INTERVAL '5 days', now() + INTERVAL '25 days', 'ACTIVE', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO benefit_product (id, sku_code, name, description, rules, status, sort) VALUES
    (1, 'COFFEE_30', '咖啡兑换券', '中杯手冲咖啡一杯，门店自取。',
     '到店出示卡密兑换；卡密仅展示一次，请及时保存；有效期以卡密为准。', 1, 1),
    (2, 'MOVIE_1', '电影票一张', '合作影院普通厅电影票一张。',
     '在合作影院选座页面输入卡密兑换；不可退换；有效期以卡密为准。', 1, 2),
    (3, 'STORAGE_50G', '云存储 50G', '会员专属 50G 云存储扩容一年。',
     '在个人中心-云存储页面输入卡密开通；到期不自动续费。', 1, 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO plan_version (id, code, name, price_cents, duration_days, ai_image_quota,
                          ai_video_quota, benefit_quota, status, version_no) VALUES
    (1, 'PREMIUM_30', 'AI 创作会员 · 30 天', 5900, 30, 20, 3, 2, 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_template (code, name, type, category, cover_url, params, status, version_no, sort) VALUES
    ('AI_PHOTO_01', '韩系证件照', 'PHOTO', '证件照', '',
     '{"prompt":"Korean style ID photo, clean background","aspectRatio":"3:4"}'::jsonb, 1, 1, 1),
    ('AI_PHOTO_02', '商务形象照', 'PHOTO', '商务', '',
     '{"prompt":"Professional business portrait, studio light","aspectRatio":"3:4"}'::jsonb, 1, 1, 2),
    ('AI_PHOTO_03', '复古胶片写真', 'PHOTO', '写真', '',
     '{"prompt":"Vintage film portrait, warm grain","aspectRatio":"3:4"}'::jsonb, 1, 1, 3),
    ('AI_PHOTO_04', '清新日系写真', 'PHOTO', '写真', '',
     '{"prompt":"Japanese fresh style portrait, soft daylight","aspectRatio":"3:4"}'::jsonb, 1, 1, 4),
    ('AI_PHOTO_05', '高级感肖像', 'PHOTO', '肖像', '',
     '{"prompt":"Editorial portrait, dramatic light","aspectRatio":"3:4"}'::jsonb, 1, 1, 5),
    ('AI_PHOTO_06', '海边度假照', 'PHOTO', '旅拍', '',
     '{"prompt":"Seaside vacation portrait, bright sky","aspectRatio":"3:4"}'::jsonb, 1, 1, 6),
    ('AI_PHOTO_07', '校园青春照', 'PHOTO', '写真', '',
     '{"prompt":"Campus youth portrait, natural smile","aspectRatio":"3:4"}'::jsonb, 1, 1, 7),
    ('AI_PHOTO_08', '黑白艺术照', 'PHOTO', '艺术', '',
     '{"prompt":"Black and white fine art portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 8),
    ('AI_DRESS_01', '国风汉服', 'DRESSUP', '古风', '',
     '{"prompt":"Traditional Chinese hanfu portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 11),
    ('AI_DRESS_02', '法式复古', 'DRESSUP', '复古', '',
     '{"prompt":"French vintage dress portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 12),
    ('AI_DRESS_03', '职场西装', 'DRESSUP', '职场', '',
     '{"prompt":"Modern business suit portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 13),
    ('AI_DRESS_04', '赛博朋克', 'DRESSUP', '未来', '',
     '{"prompt":"Cyberpunk neon outfit portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 14),
    ('AI_DRESS_05', '婚纱礼服', 'DRESSUP', '婚礼', '',
     '{"prompt":"Elegant wedding dress portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 15),
    ('AI_DRESS_06', '民族风情', 'DRESSUP', '民族', '',
     '{"prompt":"Ethnic costume portrait, rich texture","aspectRatio":"3:4"}'::jsonb, 1, 1, 16),
    ('AI_DRESS_07', '街头潮搭', 'DRESSUP', '街头', '',
     '{"prompt":"Streetwear fashion portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 17),
    ('AI_DRESS_08', '童话公主', 'DRESSUP', '童话', '',
     '{"prompt":"Fairy tale princess gown portrait","aspectRatio":"3:4"}'::jsonb, 1, 1, 18),
    ('AI_VIDEO_01', '照片动起来', 'VIDEO', '动态', '',
     '{"prompt":"Subtle natural motion from a portrait","duration":5}'::jsonb, 1, 1, 21),
    ('AI_VIDEO_02', '电影感运镜', 'VIDEO', '运镜', '',
     '{"prompt":"Cinematic camera push-in","duration":5}'::jsonb, 1, 1, 22),
    ('AI_VIDEO_03', '时光倒流', 'VIDEO', '创意', '',
     '{"prompt":"Time reverse creative transition","duration":5}'::jsonb, 1, 1, 23),
    ('AI_VIDEO_04', '节日祝福', 'VIDEO', '节日', '',
     '{"prompt":"Festival greeting motion card","duration":5}'::jsonb, 1, 1, 24)
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('plan:list', 'plan:create', 'plan:update', 'plan:delete',
               'order:list', 'order:refund', 'payment:list')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 50 AND 506
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('ai:template:list', 'ai:template:create', 'ai:template:update',
               'ai:template:delete', 'ai:task:list', 'ai:usage:list')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 60 AND 605
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('ticket:list', 'ticket:handle',
               'content:list', 'content:update', 'content:publish', 'content:rollback',
               'stats:daily', 'stats:export')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id BETWEEN 70 AND 901
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('sys_dept', 'id'), (SELECT MAX(id) FROM sys_dept));
SELECT setval(pg_get_serial_sequence('sys_role', 'id'), (SELECT MAX(id) FROM sys_role));
SELECT setval(pg_get_serial_sequence('sys_permission', 'id'), (SELECT MAX(id) FROM sys_permission));
SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), (SELECT MAX(id) FROM sys_menu));
SELECT setval(pg_get_serial_sequence('sys_user', 'id'), (SELECT MAX(id) FROM sys_user));
SELECT setval(pg_get_serial_sequence('pm_member', 'id'), (SELECT MAX(id) FROM pm_member));
SELECT setval(pg_get_serial_sequence('pm_member_period', 'id'), (SELECT MAX(id) FROM pm_member_period));
SELECT setval(pg_get_serial_sequence('benefit_product', 'id'), (SELECT MAX(id) FROM benefit_product));
SELECT setval(pg_get_serial_sequence('plan_version', 'id'), (SELECT MAX(id) FROM plan_version));
SELECT setval(pg_get_serial_sequence('ai_template', 'id'), (SELECT MAX(id) FROM ai_template));
