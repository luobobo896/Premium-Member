-- Development seed data. Passwords are development-only and must never be reused in production.

INSERT INTO sys_dept (id, parent_id, name, leader, phone, sort, status) VALUES
    (1, 0, '总公司', 'Admin', '13800000001', 1, 1),
    (2, 1, '运营部', 'Operator', '13800000002', 2, 1),
    (3, 1, '技术部', 'Viewer', '13800000003', 3, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, code, name, data_scope, status, sort, remark) VALUES
    (1, 'super_admin', '超级管理员', 'ALL', 1, 1, '拥有全部菜单和权限'),
    (2, 'operator', '运营人员', 'DEPT', 1, 2, '会员日常运营'),
    (3, 'viewer', '只读人员', 'SELF', 1, 3, '只读查询'),
    (4, 'dept_manager', '部门管理员', 'DEPT', 1, 4, '仅本部门会员与用户'),
    (5, 'self_manager', '个人管理员', 'SELF', 1, 5, '仅本人数据')
ON CONFLICT (id) DO NOTHING;

UPDATE sys_role SET data_scope = 'ALL' WHERE code = 'super_admin' AND data_scope <> 'ALL';
UPDATE sys_role SET data_scope = 'DEPT' WHERE code = 'operator' AND data_scope <> 'DEPT';
UPDATE sys_role SET data_scope = 'SELF' WHERE code = 'viewer' AND data_scope <> 'SELF';

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
    (23, 'system:log:list', '操作日志', 'API', '查询操作日志')
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
     'lucide:copyright', NULL, 9999, 1, 1, 0, 0, 0)
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
     'Self Manager', '', '个人数据权限验收账号', '/members', 'self_manager@example.com', '13800000005', 2, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2), (3, 3), (4, 4), (5, 5)
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
     now() - INTERVAL '2 days', 'SELF 验收样例')
ON CONFLICT (id) DO NOTHING;

UPDATE pm_member SET owner_id = id WHERE owner_id IS NULL AND id IN (1, 2, 3, 4, 5);

SELECT setval(pg_get_serial_sequence('sys_dept', 'id'), (SELECT MAX(id) FROM sys_dept));
SELECT setval(pg_get_serial_sequence('sys_role', 'id'), (SELECT MAX(id) FROM sys_role));
SELECT setval(pg_get_serial_sequence('sys_permission', 'id'), (SELECT MAX(id) FROM sys_permission));
SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), (SELECT MAX(id) FROM sys_menu));
SELECT setval(pg_get_serial_sequence('sys_user', 'id'), (SELECT MAX(id) FROM sys_user));
SELECT setval(pg_get_serial_sequence('pm_member', 'id'), (SELECT MAX(id) FROM pm_member));
