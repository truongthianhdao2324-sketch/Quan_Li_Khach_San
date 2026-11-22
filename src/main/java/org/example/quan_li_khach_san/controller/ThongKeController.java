package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.HoaDon;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.HoaDonRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
public class ThongKeController {

    @Autowired
    private KhachHangRepository khachHangRepository;

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

        // 3. Tính tổng doanh thu & doanh thu tháng này bằng code Java từ danh sách hóa đơn đã thanh toán
        try {
            List<HoaDon> dsHoaDon = hoaDonRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDateTime dauThang = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime cuoiThang = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

            double tongJava = 0.0;
            double thangJava = 0.0;
            long daThanhToanJava = 0L;

            if (dsHoaDon != null) {
                for (HoaDon hd : dsHoaDon) {
                    if (hd == null) continue;
                    if (!"Đã thanh toán".equalsIgnoreCase(hd.getTrangThai())) continue;

                    daThanhToanJava++;
                    double tien = (hd.getTongTien() != null) ? Math.max(0.0, hd.getTongTien()) : 0.0;
                    tongJava += tien;

                    if (hd.getNgayLap() != null
                            && !hd.getNgayLap().isBefore(dauThang)
                            && !hd.getNgayLap().isAfter(cuoiThang)) {
                        thangJava += tien;
                    }
                }
            }

            tongDoanhThu = tongJava;
            doanhThuThangNay = thangJava;
            hoaDonDaThanhToan = daThanhToanJava;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đẩy toàn bộ dữ liệu thống kê vào Model cho Thymeleaf
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
        TaiKhoan user = (session != null) ? (TaiKhoan) session.getAttribute("user") : null;
        if (user == null) {
            return "redirect:/dang-nhap";
        }
        String vaiTro = user.getVaiTro() != null ? user.getVaiTro().trim() : "";
        if (!"ADMIN".equalsIgnoreCase(vaiTro)) {
            return "redirect:/dang-nhap";
        }
        napDuLieuThongKe(model);
        return "AdminDashboard";
    }
}