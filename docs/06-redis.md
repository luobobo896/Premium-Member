# Redis 设计与使用

Redis 用于 refresh token 白名单、access token 黑名单、权限缓存和服务端验证码。所有 key 带 `app.cache-prefix`，默认 `pm:dev`。

| Key | Value | TTL | 场景 |
|---|---|---|---|
| `pm:dev:auth:refresh:{jti}` | 用户 ID | 30 天 | 登录后写入；刷新时校验；登出删除 |
| `pm:dev:auth:revoked:{jti}` | `1` | access token 剩余寿命 | 登出时拉黑当前 access token |
| `pm:dev:auth:user-revoked:{userId}` | 登出时间戳（epoch 秒） | 2 小时 | vben logout 不带 Authorization 时，撤销该用户登出前签发的全部 access token |
| `pm:dev:auth:perm:{userId}` | 逗号分隔权限码 | 10 分钟 | 登录/刷新时缓存权限，减少多表查询 |
| `pm:dev:auth:captcha:{captchaId}` | 4 位验证码 | 5 分钟 | `/auth/captcha` 生成，登录时可选校验，校验后立即删除 |

## 行为

- refresh token 白名单是刷新接口的权威开关；Redis 不可用时刷新失败，前端回到登录页。
- access token 黑名单只保存到 token 过期，不无限增长；用户级撤销记录保留一个 access token 生命周期。
- 权限缓存读取失败会回退数据库；写缓存失败只记录日志，不阻断登录。
- 登录写 refresh 白名单失败时记录 WARN，access token 仍可短期使用，但刷新不可用。
- 验证码一次消费即删除，避免重放。

## 一致性

- 角色/权限变更后应删除 `pm:{env}:auth:perm:{userId}`。当前管理端只提供只读菜单/部门接口，没有角色写接口；后续增加角色编辑时必须在同一事务提交后调用 `PermissionCache.evict(userId)`。
- 登出只撤销当前 refresh token；如果需要“踢出全部设备”，需要按用户维护 token 集合，当前未实现。

## 运维

生产禁止 `KEYS *`。排查使用：

```bash
redis-cli --scan --pattern 'pm:*:auth:*' --count 100
redis-cli TTL 'pm:dev:auth:perm:1'
redis-cli MEMORY USAGE 'pm:dev:auth:perm:1'
```

Redis 密码、连接参数只从环境变量读取，见 `03-configuration.md`。
