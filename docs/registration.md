# 注册接口

POST /user/register，Content-Type: application/json

请求：
```json
{"username":"kefan","password":"demo12345","email":"kefan@example.com"}
```

- 用户名必填，去除首尾空白后最多50字符。
- 密码必填，至少8字符，最多72个UTF-8字节，不裁剪空格。
- 邮箱选填；填写时必须符合基本邮箱格式，最多254字符。
- 用户名及非空邮箱必须唯一；多个用户可以不填写邮箱。
- 客户端不能指定id、createTime等实体字段。
- 201：{"success":true,"message":"注册成功"}
- 400：{"success":false,"message":"具体参数错误"}
- 409：{"success":false,"message":"用户名或邮箱已被使用"}
- 500：{"success":false,"message":"注册失败，请稍后重试"}

## 数据库要求

仓库没有建表SQL，应用不会自动修改现有数据库。
部署前确认 user.password 至少能存储60字符（推荐 VARCHAR(255)）。
用户名和邮箱唯一性必须由数据库唯一索引保证；409依赖数据库唯一约束。
部署前确认 username 已有唯一索引，email 列容量至少为254字符且有唯一索引。
`docs/sql/registration-email.sql` 提供 MySQL 8.0 一次性邮箱迁移：扩容并增加唯一索引，
不会删除或改写现有用户行；有重复邮箱时会失败，需要先人工处理重复数据。
迁移不是自动执行的，已有该索引的环境不要重复执行。多个 NULL 邮箱仍被允许。
旧记录的明文密码不会自动转成哈希；当前仓库尚无登录功能，后续登录必须使用
BCryptPasswordEncoder.matches 校验，新密码不得直接用SQL等值比较。

## 验证

运行 ./mvnw test（Windows使用 .\\mvnw.cmd test）。
UserServiceTest 使用模拟 Mapper 验证参数拒绝、哈希保存与插入失败；
它不验证真实MySQL表结构或数据库唯一索引。

2026-10-17 本地验证：本地 MySQL 8.0 的 ai_knowledge 数据库已经完成邮箱迁移；
user.password 为 VARCHAR(100)，可容纳 BCrypt 哈希；username 和 email 均有唯一索引。
开启真实数据库测试后执行 `.\mvnw.cmd -B test`，12项测试全部通过：
4项 Service 测试、7项 HTTP/MySQL 集成测试及1项应用启动测试。
集成测试验证真实 HTTP 注册、BCrypt 入库、拒绝客户端指定ID、重复用户名/邮箱返回409、
非法密码/JSON返回400、254/255字符邮箱边界以及多个用户不填邮箱。
每个测试使用随机专用用户名，并逐条删除自己创建的用户记录；不清空表或删除现有用户。

PowerShell 中显式启用真实数据库测试（默认跳过）：
```powershell
$env:RUN_MYSQL_INTEGRATION = 'true'
try { .\mvnw.cmd -B test }
finally { Remove-Item Env:\RUN_MYSQL_INTEGRATION }
```
运行前将数据源指向已迁移的测试数据库，可用 SPRING_DATASOURCE_URL、
SPRING_DATASOURCE_USERNAME、SPRING_DATASOURCE_PASSWORD 环境变量覆盖配置。
本地结果不代表其他部署环境已经完成迁移；500异常映射尚无真实故障注入验证。
