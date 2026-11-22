package org.example.quan_li_khach_san.controller;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.HoaDonRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @GetMapping("/letan/dashboard")
    public String letanDashboard(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) {
            return "redirect:/dang-nhap";
        }

        long tongLuongKhach = khachHangRepository.count();
        long tongLuotDatPhong = datPhongRepository.count();
        long tongSoPhong = roomRepository.count();
        long phongConTrong = roomRepository.findSoPhongByTrangThaiTrong().size();

        long phongDangCoKhach = 0;
        long phongDangDon = 0;
        for (Object[] row : roomRepository.thongKeTheoTrangThai()) {
            String trangThai = row[0] != null ? row[0].toString().trim().toLowerCase() : "";
            long soLuong = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (trangThai.contains("khách")) {
                phongDangCoKhach += soLuong;
            } else if (trangThai.contains("dọn")) {
                phongDangDon += soLuong;
            }
        }

        long hoaDonDaThanhToan = hoaDonRepository.countByTrangThai("Đã thanh toán");
        long hoaDonChuaThanhToan = hoaDonRepository.countByTrangThai("Chưa thanh toán");

        model.addAttribute("user", user);
        model.addAttribute("tongLuongKhach", tongLuongKhach);
        model.addAttribute("tongLuotDatPhong", tongLuotDatPhong);
        model.addAttribute("tongSoPhong", tongSoPhong);
        model.addAttribute("phongConTrong", phongConTrong);
        model.addAttribute("phongDangCoKhach", phongDangCoKhach);
        model.addAttribute("phongDangDon", phongDangDon);
        model.addAttribute("hoaDonDaThanhToan", hoaDonDaThanhToan);
        model.addAttribute("hoaDonChuaThanhToan", hoaDonChuaThanhToan);

        return "ReceptionistDashboard";
    }

    @GetMapping({"/", "/dang-nhap"})
    public String hienThiTrangDangNhap() {
        return "DangNhap";
    }

    @PostMapping("/xu-ly-dang-nhap")
    public String xuLyDangNhap(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        try {
            String cleanUser = username != null ? username.trim() : "";
            String cleanPass = password != null ? password.trim() : "";

            TaiKhoan tk = entityManager.find(TaiKhoan.class, cleanUser);

            if (tk != null && tk.getMatKhau() != null && tk.getMatKhau().trim().equals(cleanPass)) {
                session.setAttribute("user", tk);

                String vaiTro = tk.getVaiTro() != null ? tk.getVaiTro().trim() : "";

                if ("ADMIN".equalsIgnoreCase(vaiTro)) {
                    return "redirect:/admin/dashboard";
                } else if ("RECEPTIONIST".equalsIgnoreCase(vaiTro) || "letan".equalsIgnoreCase(vaiTro)) {
                    return "redirect:/letan/dashboard";
                } else {
                    model.addAttribute("error", "Tài khoản này không có quyền truy cập hệ thống quản trị!");
                    return "DangNhap";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        return "DangNhap";
    }

    @GetMapping({"/dang-xuat", "/logout"})
    public String dangXuat(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/dang-nhap";
    }
}