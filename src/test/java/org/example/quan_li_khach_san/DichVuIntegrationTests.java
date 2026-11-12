package org.example.quan_li_khach_san;

import org.example.quan_li_khach_san.dto.*;
import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.repository.*;
import org.example.quan_li_khach_san.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.example.quan_li_khach_san.network.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DichVuIntegrationTests {
    @Autowired DichVuService service;
    @Autowired DichVuNotificationService notifications;
    @Autowired DichVuRepository services;
    @Autowired SuDungDichVuRepository usages;
    @Autowired DatPhongRepository stays;
    @Autowired KhachHangRepository guests;
    @Autowired RoomRepository rooms;
    @Autowired WebApplicationContext context;
    @Autowired PlatformTransactionManager transactionManager;
    Long stayId, serviceId;
    @BeforeEach void fixtures() {
        usages.deleteAll(); stays.deleteAll(); guests.deleteAll(); rooms.deleteAll(); services.deleteAll();
        Room room = new Room(); room.setSoPhong("202"); room.setTrangThai("Đã có khách"); room.setLoaiPhong("Cao cấp"); room.setGiaPhong(1000000); rooms.save(room);
        KhachHang guest = new KhachHang(); guest.setSoGiayTo("TEST-202"); guest.setHoTen("Khách thử dịch vụ"); guest.setLoaiGiayTo("CCCD"); guest.setGioiTinh("Nam"); guest.setSoDienThoai("0900000000"); guests.save(guest);
        DatPhong stay = new DatPhong(); stay.setKhachHang(guest); stay.setPhong(room); stay.setNgayNhanPhong(LocalDateTime.now().minusDays(1)); stay.setTrangThai("Đang lưu trú"); stayId = stays.save(stay).getMaDatPhong();
        DichVuForm item = new DichVuForm(); item.setTenDichVu("Nước suối"); item.setGiaDichVu(new BigDecimal("20000")); service.luuDanhMuc(null, item);
        serviceId = services.findAll().get(0).getMaDichVu();
    }
    GoiDichVuForm order(String key, int quantity) {
        GoiDichVuForm form = new GoiDichVuForm(); form.setMaDatPhong(stayId); form.setMaDichVu(serviceId); form.setSoLuong(quantity); form.setMaYeuCau(key); return form;
    }
    @Test void snapshotsSurviveRepricingAndUsedServicesCannotBeDeleted() {
        Long id = service.goiDichVu(order(UUID.randomUUID().toString(), 2), "letan");
        DichVuForm changed = new DichVuForm(); changed.setTenDichVu("Nước suối đổi tên"); changed.setGiaDichVu(new BigDecimal("25000")); service.luuDanhMuc(serviceId, changed);
        SuDungDichVu stored = usages.findById(id).orElseThrow();
        assertEquals(0, stored.getThanhTien().compareTo(new BigDecimal("40000")));
        assertEquals("Nước suối", stored.getTenDichVuLucGoi());
        assertThrows(DichVuException.class, () -> service.xoaDanhMuc(serviceId));
        service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan");
        assertEquals(0, service.tongTienTheoLuotO(stayId).compareTo(new BigDecimal("65000")));
    }
    @Test void sameRequestIsChargedOnlyOnceAcrossSixteenConcurrentCalls() throws Exception {
        String key = UUID.randomUUID().toString();
        Set<Long> ids = simultaneous(16, () -> service.goiDichVu(order(key, 2), "letan"));
        assertEquals(1, ids.size()); assertEquals(1, usages.count());
        assertEquals(0, service.tongTienTheoLuotO(stayId).compareTo(new BigDecimal("40000")));
        assertThrows(DichVuException.class, () -> service.goiDichVu(order(key, 3), "letan"));
    }
    @Test void distinctConcurrentRequestsAreAllPreserved() throws Exception {
        Set<Long> ids = simultaneous(8, () -> service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan"));
        assertEquals(8, ids.size()); assertEquals(8, usages.count());
        assertEquals(0, service.tongTienTheoLuotO(stayId).compareTo(new BigDecimal("160000")));
    }
    private Set<Long> simultaneous(int workers, Callable<Long> job) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers), start = new CountDownLatch(1);
        try {
            List<Future<Long>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) tasks.add(pool.submit(() -> { ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS)); return job.call(); }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            Set<Long> ids = new HashSet<>(); for (Future<Long> result : tasks) ids.add(result.get(30, TimeUnit.SECONDS)); return ids;
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
    @Test void retriesDoNotAddMoneyAndFailedNetworkingKeepsNotificationPending() {
        Long id = service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan");
        assertFalse(service.daGui(id)); assertFalse(notifications.guiLai(id));
        assertEquals(1, usages.count()); assertFalse(service.daGui(id));
    }
    @Test void laterStayInSameRoomHasNoChargesFromPreviousStay() {
        service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan");
        DatPhong old = stays.findById(stayId).orElseThrow(); old.setNgayTraPhong(LocalDateTime.now()); old.setTrangThai("Đã trả phòng"); stays.save(old);
        assertThrows(DichVuException.class, () -> service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan"));
        DatPhong next = new DatPhong(); next.setKhachHang(guests.findAll().get(0)); next.setPhong(rooms.findById("202").orElseThrow()); next.setNgayNhanPhong(LocalDateTime.now()); next.setTrangThai("Đang lưu trú");
        Long nextId = stays.save(next).getMaDatPhong();
        assertEquals(0, service.tongTienTheoLuotO(nextId).signum()); assertEquals(1, service.lichSu(stayId).size());
    }
    @Test void invalidQuantityAndDiscontinuedServiceAreRejectedButFreeServiceIsAllowed() {
        assertThrows(DichVuException.class, () -> service.goiDichVu(order(UUID.randomUUID().toString(), 0), "letan"));
        DichVuForm free = new DichVuForm(); free.setTenDichVu("Nước miễn phí"); free.setGiaDichVu(BigDecimal.ZERO); service.luuDanhMuc(serviceId, free);
        service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan"); assertEquals(0, service.tongTienTheoLuotO(stayId).signum());
        free.setDangKinhDoanh(false); service.luuDanhMuc(serviceId, free);
        assertThrows(DichVuException.class, () -> service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan"));
        assertEquals(1, usages.count());
    }
    @Test void unreferencedServiceCanBeDeletedAndCatalogReallyPaginatesAndSorts() {
        service.xoaDanhMuc(serviceId); assertFalse(services.existsById(serviceId));
        for (int i = 0; i < 23; i++) { DichVuForm form = new DichVuForm(); form.setTenDichVu(String.format("Thử %02d", i)); form.setGiaDichVu(BigDecimal.valueOf(i)); service.luuDanhMuc(null, form); }
        var page = service.danhSach("Thử", 0, "giaDichVu", "desc");
        assertEquals(23, page.getTotalElements()); assertEquals(10, page.getNumberOfElements());
        assertEquals(0, page.getContent().get(0).getGiaDichVu().compareTo(BigDecimal.valueOf(22)));
        assertEquals(3, service.danhSach("Thử", 2, "giaDichVu", "desc").getNumberOfElements());
        assertTrue(service.danhSach("không tồn tại", 0, "invalid-field", "asc").isEmpty());
        assertEquals(2, service.danhSach("Thử", 999, null, null).getNumber());
    }
    @Test void canonicalRequestIdsDoNotDuplicateWhenLetterCaseChanges() {
        String key = UUID.randomUUID().toString();
        Long id = service.goiDichVu(order(key.toUpperCase(Locale.ROOT), 1), "letan");
        assertEquals(id, service.goiDichVu(order(key, 1), "letan"));
        assertEquals(1, usages.count());
        assertThrows(DichVuException.class, () -> service.goiDichVu(order("1-1-1-1-1", 1), "letan"));
    }
    @Test void historyPaginationKeepsTheTotalForTheWholeStay() throws Exception {
        for (int i = 0; i < 21; i++) service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan");
        assertEquals(10, service.lichSuPhanTrang(stayId, 0).getNumberOfElements());
        assertEquals(10, service.lichSuPhanTrang(stayId, 1).getNumberOfElements());
        assertEquals(1, service.lichSuPhanTrang(stayId, 999).getNumberOfElements());
        assertEquals(21, service.lichSu(stayId).size());
        assertEquals(0, service.tongTienTheoLuotO(stayId).compareTo(new BigDecimal("420000")));
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/dich-vu/lich-su").param("maDatPhong", stayId.toString()).param("page", "1").session(login("RECEPTIONIST")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("420.000 đ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang 2/3")));
    }
    @Test void fractionalPricesAreRenderedWithoutRoundingAwayStoredMoney() throws Exception {
        DichVuForm price = new DichVuForm(); price.setTenDichVu("Dịch vụ giá lẻ"); price.setGiaDichVu(new BigDecimal("20000.25"));
        service.luuDanhMuc(serviceId, price);
        service.goiDichVu(order(UUID.randomUUID().toString(), 2), "letan");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/dich-vu").session(login("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20.000,25 đ")));
        mvc.perform(get("/dich-vu/lich-su").param("maDatPhong", stayId.toString()).session(login("RECEPTIONIST")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("40.000,50 đ")));
    }
    @Test void retryRejectsWrongStayAndMalformedUrlsHaveFriendlyHttpErrors() throws Exception {
        Long id = service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan");
        assertThrows(DichVuException.class, () -> notifications.guiLai(id, stayId + 1));
        assertFalse(service.daGui(id)); assertEquals(1, usages.count());
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession user = login("RECEPTIONIST");
        mvc.perform(post("/dich-vu/su-dung/" + id + "/gui-lai").param("maDatPhong", Long.toString(stayId + 1)).param("_csrf", "test-csrf").session(user))
                .andExpect(status().isConflict()).andExpect(view().name("LoiDichVu"));
        mvc.perform(get("/dich-vu/lich-su").param("maDatPhong", "abc").session(user))
                .andExpect(status().isBadRequest()).andExpect(view().name("LoiDichVu"));
        mvc.perform(get("/dich-vu/lich-su").param("maDatPhong", "9223372036854775807").session(user))
                .andExpect(status().isNotFound()).andExpect(view().name("LoiDichVu"));
        mvc.perform(post("/dich-vu/luu").param("_csrf", "test-csrf").param("tenDichVu", "Dịch vụ").param("giaDichVu", "abc").session(login("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Đơn giá phải là số hợp lệ")));
    }
    @Test void checkoutLockPreventsAChargeFromBeingAddedAfterStayCloses() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), calling = new CountDownLatch(1);
        try {
            Future<?> checkout = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                DatPhong stay = stays.findLockedById(stayId).orElseThrow(); locked.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                stay.setNgayTraPhong(LocalDateTime.now()); stay.setTrangThai("Đã trả phòng"); stays.save(stay);
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            Future<Long> order = pool.submit(() -> { calling.countDown(); return service.goiDichVu(order(UUID.randomUUID().toString(), 1), "letan"); });
            assertTrue(calling.await(10, TimeUnit.SECONDS)); release.countDown(); checkout.get(10, TimeUnit.SECONDS);
            ExecutionException error = assertThrows(ExecutionException.class, () -> order.get(10, TimeUnit.SECONDS));
            assertInstanceOf(DichVuException.class, error.getCause()); assertEquals(0, usages.count());
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
    }
    @Test void retrySendsStoredSnapshotToThreeRealClientsWithoutCreatingAnotherCharge() throws Exception {
        Long id = service.goiDichVu(order(UUID.randomUUID().toString(), 2), "letan");
        try (DichVuSocketServer server = new DichVuSocketServer("127.0.0.1", 0, "notify-test")) {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            Future<?> serving = pool.submit(() -> { try { server.serve(); } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); } });
            List<DichVuSocketTests.Client> clients = new ArrayList<>();
            try {
                var notifier = new DichVuNotificationService(usages, transactionManager, "127.0.0.1", server.getPort(), "notify-test");
                assertFalse(notifier.guiLai(id)); assertFalse(service.daGui(id));
                for (int i = 0; i < 3; i++) { var client = new DichVuSocketTests.Client(server.getPort()); clients.add(client); client.send("SUB|" + ServiceProtocol.encode("notify-test") + "|" + ServiceProtocol.encode("Client " + i)); assertEquals("READY", client.read()); }
                assertTrue(notifier.guiLai(id)); assertTrue(service.daGui(id));
                for (var client : clients) {
                    String[] fields = client.read().split("\\|", -1);
                    assertEquals(id.toString(), fields[1]); assertEquals(stayId.toString(), fields[2]); assertEquals("Nước suối", ServiceProtocol.decode(fields[4])); assertEquals("2", fields[5]);
                }
                assertEquals(1, usages.count()); assertEquals(0, service.tongTienTheoLuotO(stayId).compareTo(new BigDecimal("40000")));
            } finally { for (var client : clients) client.close(); server.close(); serving.get(5, TimeUnit.SECONDS); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
        }
    }
    private MockHttpSession login(String role) {
        MockHttpSession session = new MockHttpSession(); TaiKhoan user = new TaiKhoan(); user.setTenDangNhap(role.equals("ADMIN") ? "admin" : "letan"); user.setVaiTro(role); session.setAttribute("user", user); session.setAttribute("dichVuCsrf", "test-csrf"); return session;
    }
    @Test void rolesCsrfAndTemplatesWorkThroughMvc() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession receptionist = login("RECEPTIONIST"), admin = login("ADMIN");
        mvc.perform(get("/dich-vu")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/dich-vu/them").session(receptionist)).andExpect(status().isForbidden());
        mvc.perform(post("/dich-vu/luu").session(receptionist).param("_csrf", "test-csrf")).andExpect(status().isForbidden());
        mvc.perform(post("/dich-vu/luu").session(admin)).andExpect(status().isForbidden());
        mvc.perform(get("/dich-vu/goi").session(admin)).andExpect(status().isForbidden());
        mvc.perform(get("/dich-vu").session(admin)).andExpect(status().isOk()).andExpect(view().name("DanhSachDichVu"));
        mvc.perform(get("/dich-vu/them").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/dich-vu/" + serviceId + "/sua").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/dich-vu/goi").session(receptionist)).andExpect(status().isOk());
        service.goiDichVu(order(UUID.randomUUID().toString(), 2), "letan");
        mvc.perform(get("/dich-vu/lich-su").param("maDatPhong", stayId.toString()).session(receptionist)).andExpect(status().isOk());
        mvc.perform(post("/dich-vu/luu").session(admin).param("_csrf", "test-csrf").param("tenDichVu", "").param("giaDichVu", "-1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "tenDichVu", "giaDichVu"));
    }
    @Test void threeIndependentJavaClientsReceiveTheCommittedDatabaseOrder() throws Exception {
        String token = org.example.quan_li_khach_san.network.ServiceProtocol.token();
        var server = new org.example.quan_li_khach_san.network.DichVuSocketServer("127.0.0.1", 0, token);
        Thread accept = new Thread(() -> {try {server.serve();} catch(java.io.IOException ex){throw new RuntimeException(ex);}}, "cli-demo-server");
        var processes = new ArrayList<Process>();
        var logs = new ArrayList<java.nio.file.Path>();
        java.nio.file.Path directory = java.nio.file.Path.of("target", "cli-demo"); java.nio.file.Files.createDirectories(directory);
        accept.start();
        try {
            for(int n=1;n<=3;n++) {
                java.nio.file.Path log=directory.resolve("client-"+n+".log"); logs.add(log);
                var builder=new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"),"bin","java").toString(),
                    "-Dfile.encoding=UTF-8", "-cp", "target/classes", "org.example.quan_li_khach_san.network.DichVuSocketClient",
                    "127.0.0.1", Integer.toString(server.getPort()), "Phuc-vu-"+n);
                builder.redirectErrorStream(true).redirectOutput(log.toFile()); processes.add(builder.start());
            }
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(server.getSubscriberCount()!=3 && System.nanoTime()<deadline) Thread.sleep(20);
            assertEquals(3,server.getSubscriberCount());
            Long id=service.goiDichVu(order(UUID.randomUUID().toString(),2),"letan");
            var notifier=new DichVuNotificationService(usages,transactionManager,"127.0.0.1",server.getPort(),token);
            assertTrue(notifier.guiLai(id)); assertTrue(service.daGui(id));
            deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<deadline) {
                boolean all=true;
                for(var log:logs) all &= java.nio.file.Files.readString(log).contains("#"+id+" | Lượt "+stayId);
                if(all) break;
                Thread.sleep(20);
            }
            for(var log:logs) {
                String text=java.nio.file.Files.readString(log);
                assertTrue(text.contains("#"+id+" | Lượt "+stayId+" | Phòng 202 | Nước suối × 2"),text);
            }
            assertEquals(1,usages.count());
        } finally {
            server.close(); accept.join(3000); assertFalse(accept.isAlive());
            for(Process process:processes) {
                if(!process.waitFor(3,TimeUnit.SECONDS)) {process.destroyForcibly();assertTrue(process.waitFor(3,TimeUnit.SECONDS));}
            }
        }
    }
}
