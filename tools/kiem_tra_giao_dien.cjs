// Local UI smoke test; expects PreviewApplication :8081 and TCP server :5000.
// Run with NODE_PATH pointing to a Playwright installation, using a disposable preview database.
const { chromium } = require('playwright');
const net = require('node:net');
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const previewUrl = process.env.QLKS_PREVIEW_URL || 'http://127.0.0.1:8081';
const socketPort = Number(process.env.QLKS_SOCKET_PORT || 5000);
const encode = value => Buffer.from(value, 'utf8').toString('base64url');
async function subscribe(name) {
  const messages = [];
  const socket = net.createConnection({host:'127.0.0.1',port:socketPort});
  await new Promise((resolve,reject) => {
    let pending=''; const timer=setTimeout(()=>{socket.destroy();reject(new Error('TCP READY timeout'));},5000);
    socket.on('error',error=>{clearTimeout(timer);socket.destroy();reject(error);});
    socket.on('connect',()=>socket.write(`SUB|${encode(process.env.QLKS_SOCKET_TOKEN || 'demo-noi-bo')}|${encode(name)}\n`));
    socket.on('data',data=>{
      pending+=data.toString('utf8');
      let end;
      while((end=pending.indexOf('\n'))>=0){const line=pending.slice(0,end).trim();pending=pending.slice(end+1);if(line==='READY'){clearTimeout(timer);resolve();}else messages.push(line);}
    });
  });
  return {socket,messages};
}
async function login(page, user) {
  await page.goto(previewUrl + '/dang-nhap');
  await page.locator('[name=username]').fill(user); await page.locator('[name=password]').fill('123456');
  await Promise.all([page.waitForURL(/dashboard/),page.getByRole('button',{name:'Đăng Nhập',exact:false}).click()]);
}
(async()=>{
  const output=path.resolve('docs/anh_kiem_thu');await fs.mkdir(output,{recursive:true});
  const clients=[];let browser;
  try{
    for(let i=1;i<=3;i++) clients.push(await subscribe(`UI-${i}`));
    browser=await chromium.launch({executablePath:process.env.QLKS_CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
    const admin=await browser.newContext({viewport:{width:1440,height:1000}});const page=await admin.newPage();
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await login(page,'admin');await page.goto(previewUrl + '/dich-vu');
    await page.getByRole('heading',{name:'Danh mục dịch vụ',exact:true}).waitFor();
    await page.screenshot({path:path.join(output,'danh_muc_desktop.png'),fullPage:true});
    await page.getByRole('link',{name:'+ Thêm dịch vụ',exact:true}).click();
    await page.locator('#tenDichVu').fill('Trà sen');await page.locator('#giaDichVu').fill('12345.25');
    await page.getByRole('button',{name:'Lưu dịch vụ',exact:true}).click();await page.getByText('Đã lưu dịch vụ.',{exact:true}).waitFor();
    await page.locator('#q').fill('Trà sen');await page.getByRole('button',{name:'Tìm kiếm',exact:true}).click();
    assert.equal(await page.locator('tbody tr').count(),1);await page.getByText('Trà sen',{exact:true}).waitFor();
    await page.getByText('12.345,25 đ',{exact:true}).waitFor();
    await page.setViewportSize({width:390,height:844});await page.goto(previewUrl + '/dich-vu');
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1));
    await page.screenshot({path:path.join(output,'danh_muc_mobile.png'),fullPage:true});
    const reception=await browser.newContext({viewport:{width:1280,height:900}});const guestPage=await reception.newPage();
    guestPage.on('pageerror',e=>errors.push(e.message));
    await login(guestPage,'letan');await guestPage.goto(previewUrl + '/dich-vu/goi');
    await guestPage.locator('#maDatPhong').selectOption({index:1});await guestPage.locator('#maDichVu').selectOption({label:'Nước suối tinh khiết · 20.000 đ'});await guestPage.locator('#soLuong').fill('2');
    await guestPage.screenshot({path:path.join(output,'goi_dich_vu.png'),fullPage:true});
    await guestPage.getByRole('button',{name:'Xác nhận gọi dịch vụ',exact:true}).click();await guestPage.getByRole('heading',{name:'Lịch sử dịch vụ theo lượt ở',exact:true}).waitFor();
    await guestPage.getByText('Đã chuyển tới client',{exact:true}).waitFor();
    assert.ok((await guestPage.locator('body').innerText()).includes('40.000 đ'));
    assert.equal(clients.every(c=>c.messages.some(m=>m.startsWith('SERVICE|'))),true);
    await guestPage.screenshot({path:path.join(output,'lich_su_dich_vu.png'),fullPage:true});
    const denied=await guestPage.goto(previewUrl + '/dich-vu/them');assert.equal(denied.status(),403);
    const badLookup=await guestPage.goto(previewUrl + '/dich-vu/lich-su?maDatPhong=abc');assert.equal(badLookup.status(),400);
    await guestPage.getByRole('heading',{name:'Chưa thể thực hiện thao tác',exact:true}).waitFor();
    assert.deepEqual(errors,[]);
    await fs.writeFile(path.join(output,'ket_qua_ui.json'),JSON.stringify({passed:true,checkedAt:new Date().toISOString(),previewUrl,database:'H2 disposable preview',desktop:'1440x1000',mobile:'390x844',tcpClients:3,received:clients.map(c=>c.messages),checked:['login','catalog render','create service','fractional price display','search','mobile overflow','order 2 x 20000','history','DB to TCP three clients','receptionist denied admin route','friendly HTTP 400 lookup error']},null,2));
    console.log('UI PASS: desktop/mobile, CRUD/search, charge 40,000, three TCP clients, permission.');
  }finally{for(const c of clients)c.socket.destroy();if(browser)await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
