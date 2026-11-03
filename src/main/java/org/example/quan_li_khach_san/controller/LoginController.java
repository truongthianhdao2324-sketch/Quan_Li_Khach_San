package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.persistence.EntityManager;
import org.example.quan_li_khach_san.entity.TaiKhoan;
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

    // 1. Mở trang đăng nhập
    @GetMapping({"/", "/dang-nhap"})
    public String hienThiTrangDangNhap() {
        return "DangNhap";
    }

    // 2. Xử lý logic khi người dùng bấm nút Đăng nhập
    @PostMapping("/xu-ly-dang-nhap")
    public String xuLyDangNhap(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        try {
            TaiKhoan tk = entityManager.find(TaiKhoan.class, username);

            if (tk != null && tk.getMatKhau().equals(password)) {
                // Lưu thông tin người dùng vào Session
                session.setAttribute("user", tk);

                String vaiTro = tk.getVaiTro() != null ? tk.getVaiTro().trim() : "";

                // Phân quyền chuẩn xác
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

        // Nếu đăng nhập sai
        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        return "DangNhap";
    }

    // 3. Đăng xuất
    @GetMapping("/dang-xuat")
    public String dangXuat(HttpSession session) {
        session.invalidate(); // Hủy session
        return "redirect:/dang-nhap";
    }

    // --- DASHBOARD CHO ADMIN ---
    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            return "redirect:/dang-nhap";
        }
        return "AdminDashboard";
    }

    // --- DASHBOARD CHO LỄ TÂN ---
    @GetMapping("/letan/dashboard")
    public String letanDashboard(HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) {
            return "redirect:/dang-nhap";
        }
        return "ReceptionistDashboard";
    }
}