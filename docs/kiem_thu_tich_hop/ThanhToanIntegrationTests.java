package org.example.quan_li_khach_san;

import org.example.quan_li_khach_san.dto.*;
import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.repository.*;
import org.example.quan_li_khach_san.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:billing-test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
class ThanhToanIntegrationTests {
    @Autowired ThanhToanService billing;
    @Autowired DichVuService ordering;
    @Autowired HoaDonRepository bills;
    @Autowired DatPhongRepository stays;
    @Autowired RoomRepository rooms;
    @Autowired KhachHangRepository guests;
    @Autowired DichVuRepository services;
    @Autowired SuDungDichVuRepository usages;
    @Autowired WebApplicationContext context;
    @Autowired PlatformTransactionManager tx;
    Long stayId, serviceId;
    @BeforeEach void setup() {
        bills.deleteAll(); usages.deleteAll(); stays.deleteAll(); guests.deleteAll(); rooms.deleteAll(); services.deleteAll();
        Room room = new Room(); room.setSoPhong("202"); room.setLoaiPhong("Cao cấp"); room.setTrangThai("Đã có khách"); room.setGiaPhong(1000000.25); rooms.save(room);
        KhachHang guest = new KhachHang(); guest.setSoGiayTo("BILL-202"); guest.setHoTen("Khách kiểm thử hóa đơn"); guests.save(guest);
        DatPhong stay = new DatPhong(); stay.setPhong(room); stay.setKhachHang(guest); stay.setNgayNhanPhong(LocalDateTime.now().minusHours(25)); stay.setTrangThai("Đang lưu trú"); stayId = stays.save(stay).getMaDatPhong();
        DichVuForm item = new DichVuForm(); item.setTenDichVu("Nước suối"); item.setGiaDichVu(new BigDecimal("20000.25")); ordering.luuDanhMuc(null, item); serviceId = services.findAll().get(0).getMaDichVu();
    }
    GoiDichVuForm order() {
        GoiDichVuForm form = new GoiDichVuForm(); form.setMaDatPhong(stayId); form.setMaDichVu(serviceId); form.setSoLuong(2); form.setMaYeuCau(UUID.randomUUID().toString()); return form;
    }
    MockHttpSession login() {
        MockHttpSession session = new MockHttpSession(); TaiKhoan user = new TaiKhoan(); user.setTenDangNhap("letan"); user.setVaiTro("RECEPTIONIST"); session.setAttribute("user",user); session.setAttribute("dichVuCsrf","billing-csrf"); return session;
    }
    @Test void previewThenNewChargeAndTamperedFormStillUsesCommittedServerTotal() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build(); MockHttpSession session = login();
        mvc.perform(get("/hoadon/checkout/202").session(session)).andExpect(status().isOk());
        ordering.goiDichVu(order(),"letan");
        mvc.perform(post("/hoadon/thanhtoan").session(session).param("_csrf","billing-csrf").param("maDatPhong",stayId.toString())
            .param("tienPhong","1").param("tienDichVu","0").param("tongTien","1").param("maHoaDon","123"))
            .andExpect(status().is3xxRedirection());
        HoaDon bill = bills.findByMaDatPhong(stayId).orElseThrow();
        assertEquals(0,bill.getTienDichVu().compareTo(new BigDecimal("40000.50")));
        assertEquals(0,bill.getTongTien().compareTo(new BigDecimal("1040000.75")));
        assertEquals("Còn trống",rooms.findById("202").orElseThrow().getTrangThai());
        assertThrows(DichVuException.class,()->ordering.goiDichVu(order(),"letan"));
        mvc.perform(get("/hoadon/chitiet/"+bill.getMaHoaDon()).session(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("40.000,50 đ")));
        mvc.perform(get("/hoadon/danhsach").param("tuKhoa","202").session(session)).andExpect(status().isOk());
        mvc.perform(post("/hoadon/chitiet/"+bill.getMaHoaDon()+"/in").session(session).param("_csrf","billing-csrf").param("tenLeTan",""))
            .andExpect(status().isOk()).andExpect(model().attribute("choPhepIn",false));
    }
    @Test void twoSimultaneousPaymentsProduceOneBill() throws Exception {
        ordering.goiDichVu(order(),"letan");
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Long> pay=()->{assertTrue(start.await(5,TimeUnit.SECONDS));return billing.thanhToan(stayId);};
            Future<Long> a=pool.submit(pay), b=pool.submit(pay); start.countDown();
            assertEquals(a.get(15,TimeUnit.SECONDS), b.get(15,TimeUnit.SECONDS)); assertEquals(1,bills.count());
        } finally {pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void orderCommittedWhileCheckoutWaitsIsIncludedInFinalBill() throws Exception {
        ExecutorService pool=Executors.newSingleThreadExecutor(); CountDownLatch locked=new CountDownLatch(1), release=new CountDownLatch(1);
        try {
            Future<?> charge=pool.submit(()->new TransactionTemplate(tx).executeWithoutResult(status->{
                stays.findLockedById(stayId).orElseThrow(); locked.countDown();
                try {assertTrue(release.await(5,TimeUnit.SECONDS));} catch(InterruptedException e){throw new RuntimeException(e);}
                ordering.goiDichVu(order(),"letan");
            }));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            CompletableFuture<Long> paid=CompletableFuture.supplyAsync(()->billing.thanhToan(stayId));
            try { assertThrows(TimeoutException.class,()->paid.get(200,TimeUnit.MILLISECONDS)); }
            finally {release.countDown();}
            charge.get(10,TimeUnit.SECONDS);
            HoaDon bill=bills.findById(paid.get(10,TimeUnit.SECONDS)).orElseThrow();
            assertEquals(0,bill.getTienDichVu().compareTo(new BigDecimal("40000.50")));
        } finally {release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void paidStayDoesNotLeakChargesToNextStayAndBillingRequiresLoginAndCsrf() throws Exception {
        ordering.goiDichVu(order(),"letan"); billing.thanhToan(stayId);
        DatPhong next=new DatPhong(); next.setPhong(rooms.findById("202").orElseThrow()); next.setKhachHang(guests.findAll().get(0)); next.setNgayNhanPhong(LocalDateTime.now()); next.setTrangThai("Đang lưu trú"); stays.save(next);
        assertEquals(0,billing.xemTruoc("202").getTienDichVu().signum());
        MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/hoadon/danhsach")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/hoadon/thanhtoan").session(login()).param("maDatPhong",next.getMaDatPhong().toString())).andExpect(status().isForbidden());
        assertEquals(1,bills.count());
    }
}
