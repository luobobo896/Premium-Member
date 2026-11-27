# 会员模块示例

模块：`admin-server/admin-member`。这是完整的 Controller/Service/Mapper/Entity/DTO/VO 示例。

## 表

`pm_member`：会员编号、登录名、昵称、手机号、等级、积分、余额、状态、入会时间、备注、审计时间、逻辑删除。

## 接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/members?page=1&pageSize=20` | `member:list` |
| GET | `/api/members/{id}` | `member:read` |
| POST | `/api/members` | `member:create` |
| PUT | `/api/members/{id}` | `member:update` |
| DELETE | `/api/members/{id}` | `member:delete` |

## 创建

```json
{
  "username": "alice",
  "nickname": "Alice",
  "phone": "13911112222",
  "level": "GOLD",
  "points": 100,
  "balance": 12.34,
  "status": 1,
  "remark": "created by curl"
}
```

响应：

```json
{
  "code": 0,
  "data": {
    "id": 4,
    "memberNo": "M20261127139166",
    "username": "alice",
    "nickname": "Alice",
    "phone": "13911112222",
    "level": "GOLD",
    "points": 100,
    "balance": 12.34,
    "status": 1,
    "joinedAt": "2026-11-27T02:18:51.759624Z",
    "remark": "created by curl",
    "createdAt": "2026-11-27T02:18:51.759624Z",
    "updatedAt": "2026-11-27T02:18:51.759624Z"
  },
  "error": null,
  "message": "ok"
}
```

## 分页

实测 `GET /api/members?page=1&pageSize=2&keyword=member` 返回 `total=3`、`items=2`，`items[0].memberNo=M202611270003`。

## 实现要点

- `MemberQuery extends PageQuery`，直接接收 vben 的 `page/pageSize`。
- `MemberMapper` 用 default 方法构建 `LambdaQueryWrapper`，Service 不出现 Wrapper。
- 创建时生成 `M + yyyyMMdd + 6 位随机数` 会员编号，冲突时重试；落库仍由 partial unique index 兜底。
- 更新检查 `updateById` 返回行数；0 行抛 `MEMBER_NOT_FOUND`，不静默成功。
- 删除使用 MyBatis-Plus `@TableLogic`，SQL 实际执行 `UPDATE deleted=1`。
- 金额和积分在 DTO 校验，数据库再通过 CHECK 约束兜底。
- 权限由 `@PreAuthorize` 在服务端执行；只读角色 `viewer` 调用 POST 实测返回 403。
