package org.example.quan_li_khach_san;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.dto.DichVuForm;
import org.example.quan_li_khach_san.repository.*;
import org.example.quan_li_khach_san.service.DichVuService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Disposable H2 UI preview, only on test classpath. NOT SQL Server acceptance. */
public class PreviewApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(QuanLiKhachSanApplication.class, args);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> {
            EntityManager em = context.getBean(EntityManager.class);
            for (String name : new String[]{"admin", "letan"}) {
                TaiKhoan user = new TaiKhoan(); user.setTenDangNhap(name); user.setMatKhau("123456"); user.setVaiTro(name.equals("admin") ? "ADMIN" : "RECEPTIONIST"); em.persist(user);
            }
            Room room = new Room(); room.setSoPhong("202"); room.setTrangThai("Đã có khách"); room.setLoaiPhong("Cao cấp"); room.setGiaPhong(1000000); em.persist(room);
            KhachHang guest = new KhachHang(); guest.setHoTen("Nguyễn Văn An"); guest.setSoGiayTo("DEMO-202"); guest.setLoaiGiayTo("CCCD"); guest.setGioiTinh("Nam"); guest.setSoDienThoai("0900000000"); em.persist(guest);
            DatPhong stay = new DatPhong(); stay.setKhachHang(guest); stay.setPhong(room); stay.setNgayNhanPhong(LocalDateTime.now().minusDays(1)); stay.setTrangThai("Đang lưu trú"); em.persist(stay);
        });
        DichVuService service = context.getBean(DichVuService.class);
        String[] names = {"Nước suối tinh khiết", "Nước ngọt có ga", "Cà phê rang xay", "Giặt ủi quần áo", "Ăn sáng tại phòng", "Trà thảo mộc", "Set trái cây", "Đưa đón sân bay", "Thuê xe máy", "Massage thư giãn", "Giữ hành lý", "Bãi đỗ xe", "Nước ép cam"};
        int[] prices = {20000,25000,30000,50000,70000,25000,80000,200000,150000,300000,0,0,40000};
        for (int i = 0; i < names.length; i++) { DichVuForm form = new DichVuForm(); form.setTenDichVu(names[i]); form.setGiaDichVu(BigDecimal.valueOf(prices[i])); service.luuDanhMuc(null, form); }
        System.out.println("PREVIEW READY — H2 tạm thời, admin/letan mật khẩu 123456. Dữ liệu mất khi dừng.");
    }
}
