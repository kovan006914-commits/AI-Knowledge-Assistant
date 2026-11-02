const { chromium } = require('playwright');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless:true});
  const page = await browser.newPage({viewport:{width:1440,height:1000}});
  const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  const name='browser_'+Date.now();
  try {
    await page.goto('http://127.0.0.1:8080');
    await page.locator('#register-tab').click();
    await page.locator('#username').fill(name);
    await page.locator('#password').fill('BrowserTest12345');
    await page.locator('#auth-submit').click();
    await page.getByText('注册成功，请使用刚才的账号登录。').waitFor();
    await page.locator('#auth-submit').click();
    await page.locator('#new-note').waitFor();
    await page.locator('#new-note').click();
    await page.locator('#note-title').fill('Controller browser test');
    await page.locator('#note-content').fill('Controller receives HTTP requests. <script>window.injected=true</script>');
    await page.locator('#note-tags').fill('Java');
    await page.locator('#save-note').click();
    await page.getByRole('heading',{name:'Controller browser test',exact:true}).waitFor();
    assert.equal(await page.evaluate(()=>window.injected),undefined);
    await page.reload();
    await page.getByRole('heading',{name:'Controller browser test',exact:true}).waitFor();
    await page.locator('#nav-ask').click();
    await page.locator('#question').fill('Controller');
    await page.locator('#ask-submit').click();
    await page.locator('#answer-card').waitFor();
    assert.match(await page.locator('#answer-mode').innerText(),/原文检索/);
    assert.match(await page.locator('#answer-text').innerText(),/Controller/);
    await page.screenshot({path:'target/ui-smoke.png',fullPage:true});
    await page.locator('.source-link').first().click();
    await page.locator('#note-title').fill('Edited browser note');
    await page.locator('#save-note').click();
    await page.locator('#nav-notes').click();
    await page.getByRole('heading',{name:'Edited browser note',exact:true}).waitFor();
    await page.getByRole('heading',{name:'Edited browser note',exact:true}).click();
    page.once('dialog',dialog=>dialog.accept());
    await page.locator('#delete-note').click();
    await page.locator('#empty-state').waitFor();
    await page.locator('#import-file').setInputFiles({
      name:'spring.md', mimeType:'text/markdown',
      buffer:Buffer.from('# Controller\n\nController 接收 HTTP 请求。\n\n# Service\n\nService 处理业务逻辑。\n')
    });
    await page.getByText('导入成功，共生成 2 条知识。').waitFor();
    await page.getByRole('heading',{name:'spring.md - Controller',exact:true}).waitFor();
    await page.locator('#search').fill('Controller');
    await page.locator('#search-form button[type="submit"]').click();
    await page.getByRole('heading',{name:'spring.md - Controller',exact:true}).waitFor();
    await page.locator('#clear-search').click();
    await page.locator('#import-file').setInputFiles({name:'bad.exe',mimeType:'application/octet-stream',buffer:Buffer.from('x')});
    await page.getByText('导入失败：只支持 .txt、.md 和 .pdf 文件').waitFor();
    await page.locator('#import-file').setInputFiles({name:'browser.pdf',mimeType:'application/pdf',buffer:Buffer.from('JVBERi0xLjMKJZOMi54gUmVwb3J0TGFiIEdlbmVyYXRlZCBQREYgZG9jdW1lbnQgKG9wZW5zb3VyY2UpCjEgMCBvYmoKPDwKL0YxIDIgMCBSCj4+CmVuZG9iagoyIDAgb2JqCjw8Ci9CYXNlRm9udCAvSGVsdmV0aWNhIC9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIC9OYW1lIC9GMSAvU3VidHlwZSAvVHlwZTEgL1R5cGUgL0ZvbnQKPj4KZW5kb2JqCjMgMCBvYmoKPDwKL0NvbnRlbnRzIDcgMCBSIC9NZWRpYUJveCBbIDAgMCA1OTUuMjc1NiA4NDEuODg5OCBdIC9QYXJlbnQgNiAwIFIgL1Jlc291cmNlcyA8PAovRm9udCAxIDAgUiAvUHJvY1NldCBbIC9QREYgL1RleHQgL0ltYWdlQiAvSW1hZ2VDIC9JbWFnZUkgXQo+PiAvUm90YXRlIDAgL1RyYW5zIDw8Cgo+PiAKICAvVHlwZSAvUGFnZQo+PgplbmRvYmoKNCAwIG9iago8PAovUGFnZU1vZGUgL1VzZU5vbmUgL1BhZ2VzIDYgMCBSIC9UeXBlIC9DYXRhbG9nCj4+CmVuZG9iago1IDAgb2JqCjw8Ci9BdXRob3IgKGFub255bW91cykgL0NyZWF0aW9uRGF0ZSAoRDoyMDAwMDEwMTAwMDAwMCswMCcwMCcpIC9DcmVhdG9yIChhbm9ueW1vdXMpIC9LZXl3b3JkcyAoKSAvTW9kRGF0ZSAoRDoyMDAwMDEwMTAwMDAwMCswMCcwMCcpIC9Qcm9kdWNlciAoUmVwb3J0TGFiIFBERiBMaWJyYXJ5IC0gXChvcGVuc291cmNlXCkpIAogIC9TdWJqZWN0ICh1bnNwZWNpZmllZCkgL1RpdGxlICh1bnRpdGxlZCkgL1RyYXBwZWQgL0ZhbHNlCj4+CmVuZG9iago2IDAgb2JqCjw8Ci9Db3VudCAxIC9LaWRzIFsgMyAwIFIgXSAvVHlwZSAvUGFnZXMKPj4KZW5kb2JqCjcgMCBvYmoKPDwKL0ZpbHRlciBbIC9BU0NJSTg1RGVjb2RlIC9GbGF0ZURlY29kZSBdIC9MZW5ndGggMTQ2Cj4+CnN0cmVhbQpHYXBARV0qXFRvJ0xfaXNNR3BPL2svcjssNHIrTmEvYG06YzZxIj0iOUhzSk07Lk8nQmM0IjNtLmY5JFFKO21aUUwrVWReQ1pkOTFaPCd1OHJNT0dPYXVNOjRsJCIwMiddN1JrTVUtdW4qKHUvNGU5aks/ZkFKRmlXQC5OKVg3Z2dyKmJiN3FWImshTm1pX2l+PmVuZHN0cmVhbQplbmRvYmoKeHJlZgowIDgKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDYxIDAwMDAwIG4gCjAwMDAwMDAwOTIgMDAwMDAgbiAKMDAwMDAwMDE5OSAwMDAwMCBuIAowMDAwMDAwNDAyIDAwMDAwIG4gCjAwMDAwMDA0NzAgMDAwMDAgbiAKMDAwMDAwMDczMSAwMDAwMCBuIAowMDAwMDAwNzkwIDAwMDAwIG4gCnRyYWlsZXIKPDwKL0lEIApbPDFjMTc4MTk4ZmJkZmE1MWIyNTk5NWQ4OWQ0MTAyMDQzPjwxYzE3ODE5OGZiZGZhNTFiMjU5OTVkODlkNDEwMjA0Mz5dCiUgUmVwb3J0TGFiIGdlbmVyYXRlZCBQREYgZG9jdW1lbnQgLS0gZGlnZXN0IChvcGVuc291cmNlKQoKL0luZm8gNSAwIFIKL1Jvb3QgNCAwIFIKL1NpemUgOAo+PgpzdGFydHhyZWYKMTAyNgolJUVPRgo=','base64')});
    await page.getByText('导入成功，共生成 1 条知识。').waitFor();
    await page.getByRole('heading',{name:'browser.pdf',exact:true}).waitFor();
    await page.locator('#import-file').setInputFiles({name:'broken.pdf',mimeType:'application/pdf',buffer:Buffer.from('not a pdf')});
    await page.getByText('导入失败：PDF 读取失败，请确认文件完整且格式正确').waitFor();
    await page.setViewportSize({width:390,height:844});
    await page.screenshot({path:'target/ui-mobile.png',fullPage:true});
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
    await page.locator('#logout').click();
    await page.locator('#auth-form').waitFor();
    assert.deepEqual(errors,[]);
    console.log('UI smoke passed: register/login/persistence/CRUD/retrieval/file import/errors/logout/mobile/XSS text rendering.');
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});

