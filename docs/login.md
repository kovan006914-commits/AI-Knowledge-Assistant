# 会话登录

本次沿用现有 BCrypt 和 Servlet 会话，不新增依赖。
适用于同源网页或支持保存 Cookie 的客户端；未开放跨域 CORS。
后续若改跨域访问，应重新设计 CSRF/CORS 策略，不能简单放开来源。

## 接口

- POST /user/login：JSON {"username":"kefan","password":"demo12345"}；
  必须携带 X-Requested-With: XMLHttpRequest。
- 成功200：{"success":true,"message":"登录成功","user":{"id":1,"username":"kefan","email":null}}。
  服务端创建会话，客户端保存 JSESSIONID Cookie；响应不包含密码或哈希。
- 用户不存在、密码错误、旧明文密码：统一401 {"success":false,"message":"用户名或密码错误"}。
- 用户名缺失/超过50、密码空白/超过72个UTF-8字节、缺少自定义请求头：400。
- GET /user/me：携带 Cookie，200返回用户信息；无会话或过期时401，不创建新会话。
- POST /user/logout：同样需要 X-Requested-With；销毁会话，200 {"success":true,"message":"退出成功"}。
  未登录时重复退出同样成功，不创建会话。

登录成功会销毁旧会话并创建新会话，空闲超时30分钟。
登录失败不更改已有会话。当前用户信息为登录时快照；账号资料修改后的同步将在对应功能实现。
SQL使用参数绑定；部署必须保留username唯一索引。
不自动接受或迁移旧明文密码，旧账号应先重置密码。

## 验证示例（Windows使用curl.exe）

```powershell
curl.exe -i -c cookies.txt -H "Content-Type: application/json" -H "X-Requested-With: XMLHttpRequest" --data-raw '{"username":"kefan","password":"demo12345"}' http://localhost:8080/user/login
curl.exe -i -b cookies.txt http://localhost:8080/user/me
curl.exe -i -b cookies.txt -H "X-Requested-With: XMLHttpRequest" -X POST http://localhost:8080/user/logout
curl.exe -i -b cookies.txt http://localhost:8080/user/me
```

示例端口以实际启动配置为准；测试用户须先注册。
PowerShell对外部程序的JSON引号处理随版本不同；也可在Postman发送同样JSON并保留Cookie。
cookies.txt包含会话凭据，不得提交Git。

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,LoginServiceTest,AuthControllerTest" test
```

这些测试模拟Mapper/Service，不验证真实MySQL SQL和网络Cookie属性。
真实数据库联调沿用注册文档中的测试数据库要求。
生产部署使用HTTPS并设置SESSION_COOKIE_SECURE=true；当前实现未加入登录限流、
多实例共享会话或其他业务接口的统一鉴权，适合本阶段本地开发。
后续知识接口必须通过统一鉴权检查当前用户，并按用户ID隔离数据。

存储异常对注册和登录统一返回500 {"success":false,"message":"操作失败，请稍后重试"}；
原注册文档中的500示例已同步更新。

## 本次验证状态（2026-10-24）

新增4项LoginServiceTest和6项AuthControllerTest，另包含原有4项UserServiceTest，共计划运行14项。
当前助手环境为JDK 27；Maven离线解析依赖成功，但编译器在访问
log4j-api-2.25.5.jar时抛出java.nio.file.AccessDeniedException，普通及fork编译均失败。
因此本次没有取得编译通过或测试通过的结果，也没有执行真实MySQL登录联调。
请在项目Java17环境中执行上方定向测试命令后，再决定合并。

## 数据源配置

原配置包含明文数据库密码。本次改为spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}，
运行应用或真实数据库测试前，请在IDEA运行配置的环境变量中设置SPRING_DATASOURCE_PASSWORD，
值为你自己本机测试数据库密码。无需修改或提交代码中的真实密码。
定向单元测试不启动应用和数据库，因此不需要设置此变量。
本次移除不清除Git历史中的旧值；如果旧值曾被用于仍在使用的数据库，请更换对应密码。
