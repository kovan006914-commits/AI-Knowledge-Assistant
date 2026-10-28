# 知页 · AI Knowledge Assistant 1.0

一个可以在浏览器里使用的个人知识助手。Java 17 + Spring Boot + MyBatis，
默认使用 H2 文件数据库，无需先安装 MySQL 或填写 AI 密钥。

## 1.0 功能

- 注册、BCrypt 密码哈希、会话登录、退出、登录状态恢复。
- 个人知识条目新增、修改、删除；标题/正文/标签搜索和分页。
- 每个账号的数据隔离，服务端校验所有读写的归属。
- 本地知识问答：按关键词检索最近更新的200条资料，返回最多5条原文和来源。
- 可选云端 AI：配置兼容 Chat Completions 的服务后，根据检索资料生成带引用的回答。
- 桌面和手机浏览器界面、空状态、错误提示和示例知识。
- 自动化接口、权限和浏览器流程验证。

## 最快启动

准备 Java 17（建议与CI一致），在项目目录双击 `start.cmd`，或在PowerShell运行：

```powershell
.\start.cmd
```

脚本首次会通过 Maven Wrapper 构建；需要网络下载依赖。之后使用已构建的JAR。
浏览器打开 http://localhost:8080 ，点击“注册账号”，注册后登录。
进入知识库，点击“添加示例知识”，然后到“知识问答”输入“Controller 有什么作用”。

如果已经从 GitHub Actions 下载了验证产物，也可以直接运行：
```powershell
java -jar AI-Knowledge-Assistant-1.0.0.jar
```
在你希望保存数据的文件夹运行，数据库写在该工作目录的 `data/` 下。
关闭控制台或按Ctrl+C停止。重启时使用相同工作目录，知识会保留。
不要删除data目录；备份前先停止应用，再复制整个data目录。

## 开发和测试

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
java -jar target/AI-Knowledge-Assistant-1.0.0.jar
```

默认测试使用隔离的H2内存数据库，不读取或清空你的MySQL。
原MySQL注册联调测试需要显式启用，见 `docs/registration.md`。
CI额外用真实浏览器验证注册→登录→新增→刷新保留→问答→编辑→删除→退出。
实际验证结果见PR的Actions记录；构建产物保留14天，可随时重新运行工作流生成。

## 配置可选 AI

不配置时，问答只返回检索原文，界面明确标为“原文检索”，不会冒充AI生成。

在启动应用的终端或IDEA运行配置的环境变量里设置：
- `AI_API_KEY`：你自己的服务密钥，不要写进代码或截图。
- `AI_BASE_URL`：兼容服务的v1根地址，默认 `https://api.openai.com/v1`。
- `AI_MODEL`：该服务支持的模型名，默认 `gpt-4o-mini`，可修改。

每次提问会把问题与当前账号检索到的最多5条资料片段发送给配置的服务。
请确认这些资料适合发送；服务可能按其定价收费。
无匹配资料时不会调用模型；服务错误或超时则退回原文检索并显示提示。
API密钥只由服务器读取，不返回给浏览器。真实供应商调用需要你自行配置有效密钥；
自动化测试只验证兼容协议及模拟服务故障。

## 使用原有 MySQL

默认demo模式与原MySQL数据独立，原账号不会自动出现在H2中。
若要继续使用旧账号：
1. 备份数据库。
2. 在目标库执行 `docs/sql/v1-mysql.sql`；它只创建不存在的表，不删除数据。
3. 对已有user表核对password长度、username唯一索引及email唯一约束；
   必要时按旧注册迁移说明处理，不能以CREATE IF NOT EXISTS代替已有表迁移。
4. 设置 `SPRING_PROFILES_ACTIVE=mysql`、`SPRING_DATASOURCE_URL`、
   `SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 后启动。
旧明文密码不会自动迁移；应使用重新设置后的BCrypt密码或新注册账号。

## 部署边界

默认只监听127.0.0.1，面向本机个人使用。
修改 `SERVER_ADDRESS`、`SERVER_PORT` 可调整监听地址与端口。
公网部署需HTTPS并设 `SESSION_COOKIE_SECURE=true`，还需要登录防暴力破解、
数据库备份和运维方案；本版不是已经完成公网安全评估的托管服务。
会话存储在单个进程中，重启需重新登录，多实例部署需共享会话。
没有文件上传、向量数据库、密码重置或管理员后台；文本知识管理与带来源问答构成1.0范围。
接口文档见 `docs/api-v1.md`。
