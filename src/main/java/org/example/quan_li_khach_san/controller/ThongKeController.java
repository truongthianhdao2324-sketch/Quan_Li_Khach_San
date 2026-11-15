package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ThongKeController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping("/admin/thong-ke")
    public String hienThiDashboard(Model model) {
        // Lấy số lượng khách thực tế từ hệ thống của Thành viên 1
        long tongLuongKhach = khachHangRepository.count();
        model.addAttribute("tongLuongKhach", tongLuongKhach);

        // Doanh thu (có thể lấy từ repository hóa đơn của TV4 hoặc tạm gán số liệu)
        double tongDoanhThu = 48500000;
        model.addAttribute("tongDoanhThu", tongDoanhThu);

        // Tên file HTML trùng khớp với file sẵn có trong thư mục templates của bạn
        return "AdminDashboard";
    }
}