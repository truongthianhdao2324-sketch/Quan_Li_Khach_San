package org.example.quan_li_khach_san.controller;

<<<<<<< HEAD
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.HoaDonRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
=======
import org.example.quan_li_khach_san.repository.KhachHangRepository;
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

<<<<<<< HEAD
import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

=======
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
@Controller
public class ThongKeController {

    @Autowired
    private KhachHangRepository khachHangRepository;

<<<<<<< HEAD
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    public void napDuLieuThongKe(Model model) {
        long tongLuongKhach = 0L;
        long tongLuotDatPhong = 0L;
        long tongSoPhong = 0L;
        long phongConTrong = 0L;
        long phongDangCoKhach = 0L;
        long phongDangDon = 0L;
        double tongDoanhThu = 0.0;
        double doanhThuThangNay = 0.0;
        long hoaDonDaThanhToan = 0L;

        // 1. Khách hàng & đặt phòng
        try {
            tongLuongKhach = khachHangRepository.count();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            tongLuotDatPhong = datPhongRepository.count();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Phòng & trạng thái phòng
        try {
            tongSoPhong = roomRepository.count();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            List<Object[]> rows = roomRepository.thongKeTheoTrangThai();
            if (rows != null) {
                for (Object[] row : rows) {
                    if (row == null || row.length < 2) continue;
                    String trangThai = row[0] != null ? row[0].toString().trim().toLowerCase() : "";
                    long soLuong = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;

                    if (trangThai.contains("trống")) {
                        phongConTrong += soLuong;
                    } else if (trangThai.contains("khách")) {
                        phongDangCoKhach += soLuong;
                    } else if (trangThai.contains("dọn")) {
                        phongDangDon += soLuong;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (phongConTrong == 0L) {
            try {
                List<?> phongTrongList = roomRepository.findSoPhongByTrangThaiTrong();
                if (phongTrongList != null) {
                    phongConTrong = phongTrongList.size();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3. Doanh thu & hóa đơn
        try {
            Double tongDoanhThuDb = hoaDonRepository.tinhTongDoanhThu();
            tongDoanhThu = tongDoanhThuDb != null ? tongDoanhThuDb : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime dauThang = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime cuoiThang = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
            Double doanhThuThangDb = hoaDonRepository.tinhDoanhThuTheoThoiGian(dauThang, cuoiThang);
            doanhThuThangNay = doanhThuThangDb != null ? doanhThuThangDb : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            hoaDonDaThanhToan = hoaDonRepository.countByTrangThai("Đã thanh toán");
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("tongLuongKhach", tongLuongKhach);
        model.addAttribute("tongLuotDatPhong", tongLuotDatPhong);
        model.addAttribute("tongSoPhong", tongSoPhong);
        model.addAttribute("phongConTrong", phongConTrong);
        model.addAttribute("phongDangCoKhach", phongDangCoKhach);
        model.addAttribute("phongDangDon", phongDangDon);
        model.addAttribute("tongDoanhThu", tongDoanhThu);
        model.addAttribute("doanhThuThangNay", doanhThuThangNay);
        model.addAttribute("hoaDonDaThanhToan", hoaDonDaThanhToan);
    }

    @GetMapping({"/admin/dashboard", "/admin/thong-ke"})
    public String hienThiAdminDashboard(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) {
            return "redirect:/dang-nhap";
        }
        String vaiTro = user.getVaiTro() != null ? user.getVaiTro().trim() : "";
        if (!"ADMIN".equalsIgnoreCase(vaiTro)) {
            return "redirect:/dang-nhap";
        }
        napDuLieuThongKe(model);
=======
    @GetMapping("/admin/thong-ke")
    public String hienThiDashboard(Model model) {
        // Lấy số lượng khách thực tế từ hệ thống của Thành viên 1
        long tongLuongKhach = khachHangRepository.count();
        model.addAttribute("tongLuongKhach", tongLuongKhach);

        // Doanh thu (có thể lấy từ repository hóa đơn của TV4 hoặc tạm gán số liệu)
        double tongDoanhThu = 48500000;
        model.addAttribute("tongDoanhThu", tongDoanhThu);

        // Tên file HTML trùng khớp với file sẵn có trong thư mục templates của bạn
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
        return "AdminDashboard";
    }
}