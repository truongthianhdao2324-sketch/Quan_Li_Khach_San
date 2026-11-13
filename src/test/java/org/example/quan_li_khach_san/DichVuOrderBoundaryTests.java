package org.example.quan_li_khach_san;

import jakarta.persistence.EntityManager;
import org.example.quan_li_khach_san.dto.GoiDichVuForm;
import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.network.*;
import org.example.quan_li_khach_san.repository.SuDungDichVuRepository;
import org.example.quan_li_khach_san.service.DichVuOrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.*;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:qlks-boundary;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=1000"})
class DichVuOrderBoundaryTests {
    private static final DichVuSocketServer server;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Future<?> serving;
    static {
        try { server = new DichVuSocketServer("127.0.0.1", 0, "boundary"); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
        serving = executor.submit(() -> { try { server.serve(); } catch (IOException ex) { throw new UncheckedIOException(ex); } });
    }
    @DynamicPropertySource static void network(DynamicPropertyRegistry properties) {
        properties.add("qlks.socket.port", server::getPort);
        properties.add("qlks.socket.token", () -> "boundary");
    }
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired DichVuOrderService orders;
    @Autowired SuDungDichVuRepository usages;

    @AfterAll static void stop() throws Exception {
        server.close(); serving.get(5, TimeUnit.SECONDS); executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test void oneDatabaseConnectionIsEnoughAndDuplicateSubmissionDoesNotRebroadcast() throws Exception {
        GoiDichVuForm form = new TransactionTemplate(manager).execute(status -> {
            Room room = new Room(); room.setSoPhong("B202"); room.setTrangThai("Đã có khách"); room.setLoaiPhong("Cao cấp"); room.setGiaPhong(1000000); em.persist(room);
            KhachHang guest = new KhachHang(); guest.setHoTen("Khách kiểm thử"); guest.setSoGiayTo("BOUNDARY-1"); em.persist(guest);
            DatPhong stay = new DatPhong(); stay.setKhachHang(guest); stay.setPhong(room); stay.setNgayNhanPhong(LocalDateTime.now()); stay.setTrangThai("Đang lưu trú"); em.persist(stay);
            DichVu item = new DichVu(); item.setTenDichVu("Nước suối"); item.setGiaDichVu(new BigDecimal("20000")); em.persist(item); em.flush();
            GoiDichVuForm result = new GoiDichVuForm(); result.setMaDatPhong(stay.getMaDatPhong()); result.setMaDichVu(item.getMaDichVu()); result.setSoLuong(2); result.setMaYeuCau(UUID.randomUUID().toString()); return result;
        });
        try (var receiver = new DichVuSocketTests.Client(server.getPort())) {
            receiver.send("SUB|" + ServiceProtocol.encode("boundary") + "|" + ServiceProtocol.encode("Phục vụ")); assertEquals("READY", receiver.read());
            var result = orders.goiVaThongBao(form, "letan");
            assertTrue(result.daGui(), "The saved transaction must release its only DB connection before notification reads/updates.");
            assertTrue(receiver.read().startsWith("SERVICE|" + result.id() + "|"));
            assertTrue(orders.goiVaThongBao(form, "letan").daGui());
            receiver.socket.setSoTimeout(150);
            assertThrows(SocketTimeoutException.class, receiver::read);
            assertEquals(1, usages.count());
        }
        new TransactionTemplate(manager).executeWithoutResult(status ->
                assertThrows(IllegalTransactionStateException.class, () -> orders.goiVaThongBao(form, "letan")));
        assertEquals(1, usages.count());
    }
}
