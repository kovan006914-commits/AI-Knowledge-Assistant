'use strict';
const $ = id => document.getElementById(id);
let authMode = 'login', page = 1, total = 0, query = '', currentUser = null, aiConfigured = false;
const size = 20;
function text(id, value) { $(id).textContent = value; }
function message(id, value, success = false) { text(id, value); $(id).classList.toggle('success', success); }
function notice(value) { text('notice', value); $('notice').hidden = !value; }
function showAuth() {
  currentUser = null; $('app').hidden = true; $('auth').hidden = false; $('boot').hidden = true;
  if ($('editor').open) $('editor').close();
  $('notes-list').replaceChildren(); $('answer-card').hidden = true;
  text('answer-text', ''); $('sources').replaceChildren(); $('editor-form').reset(); $('question').value = '';
}
function authTab(mode) {
  authMode = mode; $('login-tab').classList.toggle('selected', mode === 'login');
  $('register-tab').classList.toggle('selected', mode === 'register'); $('email-row').hidden = mode !== 'register';
  text('auth-heading', mode === 'login' ? '欢迎回来' : '创建你的知识空间');
  text('auth-hint', mode === 'login' ? '登录后，继续整理你的知识。' : '密码至少8个字符，最多72个UTF-8字节。');
  text('auth-submit', mode === 'login' ? '登录' : '注册账号');
  $('password').autocomplete = mode === 'login' ? 'current-password' : 'new-password';
  message('auth-message', '');
}
async function api(url, options = {}) {
  const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 45000);
  try {
    const response = await fetch(url, {credentials: 'same-origin', ...options, signal: controller.signal,
      headers: {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest', ...options.headers}});
    const data = await response.json().catch(() => ({message: '服务器返回了无法识别的响应'}));
    if (!response.ok) {
      if (response.status === 401 && url !== '/user/login' && url !== '/user/me') {
        showAuth(); message('auth-message', '登录已过期，请重新登录。');
      }
      throw new Error(data.message || '操作失败，请稍后重试');
    }
    return data;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请稍后重试');
    if (error instanceof TypeError) throw new Error('无法连接服务器，请确认应用正在运行');
    throw error;
  } finally { clearTimeout(timer); }
}
async function enter(user) {
  currentUser = user; $('auth').hidden = true; $('app').hidden = false; $('boot').hidden = true;
  text('account-name', user.username); text('avatar', user.username.slice(0, 1));
  $('password').value = ''; query = ''; page = 1; $('search').value = ''; notice('');
  try {
    const status = await api('/api/status'); aiConfigured = status.aiConfigured;
    text('ai-badge', aiConfigured ? 'AI 已配置' : '本地检索');
    text('ask-mode', aiConfigured ? 'AI 知识问答' : '本地知识检索');
    text('ask-submit', aiConfigured ? '提问 ↗' : '查找答案 ↗');
    text('ask-description', aiConfigured
      ? '提问时，会把问题和最多5条相关知识片段发送到已配置的AI服务。回答附带参考来源。'
      : '按关键词检索最近更新的200条知识，返回相关原文和来源。此模式不生成AI答案。');
  } catch (error) { notice(error.message); }
  tab('notes'); await loadNotes();
}
function tab(name) {
  $('notes-panel').hidden = name !== 'notes'; $('ask-panel').hidden = name !== 'ask';
  $('nav-notes').classList.toggle('selected', name === 'notes');
  $('nav-ask').classList.toggle('selected', name === 'ask');
}
function node(tag, className, value) {
  const el = document.createElement(tag); if (className) el.className = className;
  if (value !== undefined) el.textContent = value; return el;
}
async function loadNotes() {
  $('notes-loading').hidden = false; $('notes-list').replaceChildren(); $('empty-state').hidden = true;
  try {
    const data = await api('/api/knowledge?q=' + encodeURIComponent(query) + '&page=' + page + '&size=' + size);
    total = data.total;
    if (page > 1 && data.items.length === 0) { page--; return await loadNotes(); }
    text('result-count', total + ' 条'); text('list-label', query ? '搜索结果' : '全部知识');
    for (const item of data.items) {
      const card = node('button', 'note-card'); card.type = 'button';
      card.append(node('h3', '', item.title), node('p', 'note-preview', item.content));
      const footer = node('footer'), tags = node('div');
      for (const tag of item.tags.split(/[,，]/).filter(Boolean).slice(0, 2)) tags.append(node('span', 'pill', tag.trim()));
      footer.append(tags, node('time', '', (item.updatedAt || '').slice(0, 10))); card.append(footer);
      card.addEventListener('click', () => openNote(item.id)); $('notes-list').append(card);
    }
    $('empty-state').hidden = total > 0; $('add-examples').hidden = !!query;
    text('empty-title', query ? '没有找到相关知识' : '从第一条知识开始');
    text('empty-description', query ? '换个关键词，或清除搜索查看全部内容。' : '保存一段笔记，或先添加示例体验完整流程。');
    text('page-info', '第 ' + page + ' / ' + Math.max(1, Math.ceil(total / size)) + ' 页');
    $('previous').disabled = page <= 1; $('next').disabled = page * size >= total;
  } catch (error) { notice(error.message); } finally { $('notes-loading').hidden = true; }
}
async function openNote(id) {
  try {
    const item = id ? await api('/api/knowledge/' + id) : {title:'', tags:'', content:''};
    $('editor-form').reset(); $('note-id').value = id || ''; $('note-title').value = item.title;
    $('note-tags').value = item.tags; $('note-content').value = item.content;
    text('editor-heading', id ? '编辑知识' : '新建知识'); $('delete-note').hidden = !id;
    message('editor-message', ''); $('editor').showModal(); $('note-title').focus();
  } catch (error) { notice(error.message); }
}
$('login-tab').onclick = () => authTab('login'); $('register-tab').onclick = () => authTab('register');
$('auth-form').onsubmit = async event => {
  event.preventDefault(); $('auth-submit').disabled = true; message('auth-message', '');
  const body = {username: $('username').value, password: $('password').value};
  if (authMode === 'register') body.email = $('email').value;
  try {
    const data = await api('/user/' + (authMode === 'login' ? 'login' : 'register'), {method:'POST', body:JSON.stringify(body)});
    if (authMode === 'register') { authTab('login'); message('auth-message', '注册成功，请使用刚才的账号登录。', true); }
    else await enter(data.user);
  } catch (error) { message('auth-message', error.message); } finally { $('auth-submit').disabled = false; }
};
$('logout').onclick = async () => {
  try { await api('/user/logout', {method:'POST'}); showAuth(); authTab('login'); }
  catch (error) { notice(error.message); }
};
$('nav-notes').onclick = () => tab('notes'); $('nav-ask').onclick = () => tab('ask');
$('new-note').onclick = () => openNote(null);
$('close-editor').onclick = $('cancel-editor').onclick = () => $('editor').close();
$('editor-form').onsubmit = async event => {
  event.preventDefault(); $('save-note').disabled = true; message('editor-message', '');
  const id = $('note-id').value;
  const body = {title:$('note-title').value, content:$('note-content').value, tags:$('note-tags').value};
  try { await api('/api/knowledge' + (id ? '/' + id : ''), {method:id ? 'PUT' : 'POST', body:JSON.stringify(body)});
    $('editor').close(); notice('已保存'); await loadNotes();
  } catch (error) { message('editor-message', error.message); } finally { $('save-note').disabled = false; }
};
$('delete-note').onclick = async () => {
  if (!confirm('确定删除这条知识吗？删除后无法恢复。')) return;
  $('delete-note').disabled = true;
  try { await api('/api/knowledge/' + $('note-id').value, {method:'DELETE'}); $('editor').close(); notice('已删除'); await loadNotes(); }
  catch (error) { message('editor-message', error.message); } finally { $('delete-note').disabled = false; }
};
$('search-form').onsubmit = event => { event.preventDefault(); query = $('search').value.trim(); page = 1; notice(''); loadNotes(); };
$('clear-search').onclick = () => { query=''; page=1; $('search').value=''; notice(''); loadNotes(); };
$('previous').onclick = () => { if (page > 1) {page--; loadNotes();} };
$('next').onclick = () => { if (page * size < total) {page++; loadNotes();} };
$('add-examples').onclick = async () => {
  $('add-examples').disabled = true;
  const examples = [
    {title:'Spring Boot：Controller 的作用',tags:'Java, Spring Boot',content:'Controller 是接收 HTTP 请求的入口。@RestController 将返回值作为响应正文。@PostMapping 定义接收 POST 请求的方法。业务规则放在 Service，数据库操作交给 Mapper。'},
    {title:'项目笔记：如何验证登录功能',tags:'项目, 测试',content:'先注册账号，再登录。登录成功后服务器建立会话，浏览器保存 Cookie。访问当前用户接口确认身份；退出登录后再次访问，应当返回401。密码使用 BCrypt 哈希保存。'},
    {title:'学习方法：给知识留下上下文',tags:'学习方法',content:'保存知识时写清楚它解决了什么问题、适用场景和一个例子。标题使用具体关键词，方便日后检索。定期更新内容，比保存很多无法使用的资料更有帮助。'}
  ];
  try { for (const example of examples) await api('/api/knowledge',{method:'POST',body:JSON.stringify(example)});
    notice('示例已添加，可以前往知识问答试一试。'); await loadNotes();
  } catch (error) { notice(error.message); } finally { $('add-examples').disabled=false; }
};
$('ask-form').onsubmit = async event => {
  event.preventDefault(); $('ask-submit').disabled=true; message('answer-status','正在检索资料，请稍候…',true);
  $('answer-card').hidden=true;
  try {
    const answer = await api('/api/ask',{method:'POST',body:JSON.stringify({question:$('question').value})});
    text('answer-text', answer.answer); text('answer-mode', answer.mode === 'ai' ? 'AI 回答' : '原文检索');
    text('answer-warning', answer.warning || ''); $('sources').replaceChildren();
    $('sources-heading').hidden = answer.sources.length === 0;
    answer.sources.forEach((source, i) => {
      const button=node('button','source-link','['+(i+1)+'] '+source.title+' ↗'); button.type='button';
      button.onclick=()=>openNote(source.id); $('sources').append(button);
    });
    $('answer-card').hidden=false; message('answer-status','');
  } catch (error) { message('answer-status',error.message); } finally { $('ask-submit').disabled=false; }
};
(async () => {
  try { const data=await api('/user/me'); await enter(data.user); }
  catch { showAuth(); }
})();
