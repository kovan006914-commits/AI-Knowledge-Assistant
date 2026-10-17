# 注册接口

POST /user/register，Content-Type: application/json

请求：
```json
{"username":"kefan","password":"demo12345","email":"kefan@example.com"}
```

- 用户名必填，去除首尾空白后最多50字符。
- 密码必填，至少8字符，最多72个UTF-8字节，不裁剪空格。
- 邮箱选填；填写时必须符合基本邮箱格式，最多254字符。
- 客户端不能指定id、createTime等实体字段。
- 201：{"success":true,"message":"注册成功"}
- 400：{"success":false,"message":"具体参数错误"}
- 409：{"success":false,"message":"用户名或邮箱已被使用"}
- 500：{"success":false,"message":"注册失败，请稍后重试"}

## 数据库要求

仓库没有建表SQL，应用不会自动修改现有数据库。
部署前确认 user.password 至少能存储60字符（推荐 VARCHAR(255)）。
用户名唯一性必须由数据库唯一索引保证；409依赖已有唯一约束，
此改动未声称已经建立唯一约束。先检查现有数据及表结构，再添加索引。
旧记录的明文密码不会自动转成哈希；当前仓库尚无登录功能，后续登录必须使用
BCryptPasswordEncoder.matches 校验，新密码不得直接用SQL等值比较。

## 验证

运行 ./mvnw test（Windows使用 .\\mvnw.cmd test）。
UserServiceTest 使用模拟 Mapper 验证参数拒绝、哈希保存与插入失败；
它不验证真实MySQL表结构或数据库唯一索引。

2026-10-17 本地验证：执行 `.\mvnw.cmd -B -U "-Dtest=UserServiceTest" test`，编译成功，4项测试全部通过（0失败、0错误、0跳过）。真实MySQL联调尚未执行，数据库表结构、密码字段容量及唯一索引仍需在部署前确认。
