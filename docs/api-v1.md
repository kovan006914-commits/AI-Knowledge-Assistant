# 1.0 接口契约

同源网页使用Cookie会话。所有JSON请求使用Content-Type: application/json。
登录、退出、以及知识写入/提问必须提供X-Requested-With: XMLHttpRequest。
不提供跨域CORS；未来改变跨域策略时必须重新评估CSRF保护。

| 方法与路径 | 行为 | 成功 |
| --- | --- | --- |
| POST /user/register | username/password/email（选填） | 201 ApiResponse |
| POST /user/login | username/password | 200 LoginResponse，设置Cookie |
| GET /user/me | 当前会话用户 | 200 LoginResponse |
| POST /user/logout | 销毁会话 | 200 ApiResponse |
| GET /api/status | 版本与AI配置标志，不返回密钥 | 200 |
| GET /api/knowledge?q=&page=1&size=20 | 仅当前用户的列表 | 200 {items,total,page,size} |
| GET /api/knowledge/{id} | 当前用户的条目 | 200 KnowledgeItem |
| POST /api/knowledge | {title,content,tags} | 201 KnowledgeItem |
| PUT /api/knowledge/{id} | 完整替换标题/正文/标签 | 200 KnowledgeItem |
| DELETE /api/knowledge/{id} | 删除当前用户的条目 | 200 ApiResponse |
| POST /api/ask | {question} | 200 {mode,answer,sources,warning} |

ApiResponse：{success,message}。
LoginResponse：{success,message,user:{id,username,email}}。
KnowledgeItem：{id,ownerId,title,content,tags,createdAt,updatedAt}。
客户端提交ownerId无效，归属始终取自服务器会话；查询、更新、删除同时匹配id和ownerId。
别人的条目与不存在的条目都返回404。
列表页大小1—100；q最多100字符，百分号/下划线作为普通字符搜索。
标题1—120、正文1—20000、标签0—200、问题1—500（Java字符串长度计数）。
问答在当前账号最近更新的200条资料内检索最多5条，每条最多约1400字。
每账号每分钟最多10次提问，超过返回429；重启后计数清空。

常见错误：
- 400：非法参数、JSON或登录/退出缺少指定请求头。
- 401：未登录/会话过期，或用户名密码错误。
- 403：已登录的知识写入/提问缺少指定请求头。
- 404：条目不存在或不属于当前用户。
- 409：用户名或非空邮箱重复。
- 429：提问频率过高。
- 500：存储失败，返回通用错误，不暴露SQL或连接信息。

AI服务失败是可恢复情况：返回200、mode=retrieval及warning，保留原文检索结果。
mode=ai才表示模型生成内容；“AI已配置”仅表示服务器存在密钥，不证明供应商连接成功。

所有时间为服务器数据库时间的ISO本地日期时间；1.0不提供跨时区时间转换。
问答不持久化，资料会持久化。删除条目后无法从应用恢复，请先备份。
