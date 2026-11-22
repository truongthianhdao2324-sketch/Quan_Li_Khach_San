package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.example.quan_li_khach_san.service.KhachHangScheduledService;
import org.example.quan_li_khach_san.service.KhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class KhachHangController {

    @Autowired
    private KhachHangService khachHangService;

    @Autowired
    private KhachHangScheduledService khachHangScheduledService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private RoomRepository roomRepository;

    // Helper lấy tên đăng nhập (ten_dang_nhap) từ Session hoặc Principal
    private String layTenDangNhapHienTai(HttpSession session, Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (session != null) {
            String[] keys = {"tenDangNhap", "ten_dang_nhap", "username", "user", "taiKhoan", "loggedInUser", "account"};
            for (String key : keys) {
                Object val = session.getAttribute(key);
                String extracted = trichXuatTenDangNhap(val);
                if (extracted != null) return extracted;
            }
            // Quét toàn bộ attribute trong session phòng trường hợp đặt tên biến khác
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                Object val = session.getAttribute(attrNames.nextElement());
                String extracted = trichXuatTenDangNhap(val);
                if (extracted != null) return extracted;
            }
        }
        return "letan";
    }

    private String trichXuatTenDangNhap(Object val) {
        if (val == null) return null;
        if (val instanceof String str && !str.isBlank() && !str.equalsIgnoreCase("RECEPTIONIST") && !str.equalsIgnoreCase("ADMIN")) {
            return str;
        }
        try {
            Method m = val.getClass().getMethod("getTenDangNhap");
            Object res = m.invoke(val);
            if (res != null && !res.toString().isBlank()) return res.toString();
        } catch (Exception ignored) {}
        try {
            Method m = val.getClass().getMethod("getUsername");
            Object res = m.invoke(val);
            if (res != null && !res.toString().isBlank()) return res.toString();
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    // I. PHÂN HỆ LỄ TÂN (/khachhang/...) -> THÊM, SỬA, HỦY (CÓ LÝ DO), TÌM KIẾM, LỌC
    // =========================================================================

    @GetMapping({"/khachhang", "/khachhang/danhsach"})
    public String danhSachKhachHangLeTan(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "trangThai", required = false) String trangThai,
                                         Model model) {
        model.addAttribute("listKH", khachHangService.timKiemVaLocKhachHang(keyword, trangThai));
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai != null ? trangThai : "ALL");
        return "DanhSachKH";
    }

    @GetMapping("/khachhang/them")
    public String formThemKhachHangLeTan(Model model) {
        model.addAttribute("khachHang", new KhachHang());
        return "ThemKhachHang";
    }

    @PostMapping("/khachhang/them")
    public String luuKhachHangLeTan(@ModelAttribute KhachHang khachHang, RedirectAttributes ra) {
        try {
            khachHangService.luuHoacCapNhatKhachHang(khachHang);
            ra.addFlashAttribute("success", "Thêm hồ sơ khách hàng mới thành công!");
            return "redirect:/khachhang/danhsach";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/khachhang/them";
        }
    }

    @GetMapping("/khachhang/sua/{id}")
    public String formSuaKhachHangLeTan(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        KhachHang kh = khachHangRepository.findById(id).orElseThrow();
        if ("Đã hủy".equalsIgnoreCase(kh.getTrangThai())) {
            ra.addFlashAttribute("error", "Hồ sơ khách hàng đã hủy không thể chỉnh sửa!");
            return "redirect:/khachhang/danhsach";
        }
        model.addAttribute("khachHang", kh);
        return "SuaKhachHang";
    }

    @PostMapping("/khachhang/sua")
    public String capNhatKhachHangLeTan(@ModelAttribute KhachHang khachHang, RedirectAttributes ra) {
        try {
            khachHangService.luuHoacCapNhatKhachHang(khachHang);
            ra.addFlashAttribute("success", "Cập nhật thông tin khách hàng thành công!");
            return "redirect:/khachhang/danhsach";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/khachhang/sua/" + khachHang.getMaKhachHang();
        }
    }

    @PostMapping("/khachhang/huy")
    public String huyKhachHangLeTan(@RequestParam("maKhachHang") Long maKhachHang,
                                    @RequestParam("lyDoHuy") String lyDoHuy,
                                    HttpSession session,
                                    Principal principal,
                                    RedirectAttributes ra) {
        try {
            String tenDangNhapHuy = layTenDangNhapHienTai(session, principal);
            khachHangService.huyKhachHang(maKhachHang, lyDoHuy, tenDangNhapHuy);
            ra.addFlashAttribute("success", "Đã hủy hồ sơ khách hàng bởi tài khoản [" + tenDangNhapHuy + "]!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/khachhang/danhsach";
    }

//    // --- QUẢN LÝ PHÒNG ---
//
//    @GetMapping("/khachhang/phong")
//    public String hienThiDanhSachPhong(HttpSession session, Model model) {
//        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
//        if (user == null) return "redirect:/dang-nhap";
//
//        model.addAttribute("danhSachPhong", roomRepository.findAll());
//        return "DanhSachPhong";
//    }

    @GetMapping("/khachhang/phong/doi-trang-thai")
    public String doiTrangThaiPhong(@RequestParam("soPhong") String soPhong) {
        Room room = roomRepository.findById(soPhong).orElse(null);
        if (room != null) {
            String hienTai = room.getTrangThai();
            if ("Còn trống".equalsIgnoreCase(hienTai)) {
                room.setTrangThai("Đã có khách");
            } else if ("Đã có khách".equalsIgnoreCase(hienTai)) {
                room.setTrangThai("Đang dọn phòng");
            } else {
                room.setTrangThai("Còn trống");
            }
            roomRepository.save(room);
        }
        return "redirect:/khachhang/phong";
    }

    // =========================================================================
    // III. PHÂN HỆ ADMIN (/admin/khachhang/...)
    // =========================================================================

    @GetMapping({"/admin/khachhang", "/admin/khachhang/danhsach"})
    public String danhSachKhachHangAdmin(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "trangThai", required = false) String trangThai,
                                         Model model) {
        List<KhachHang> tatCa = khachHangRepository.findAll();
        long tongKhach = tatCa.size();
        long dangHoatDong = tatCa.stream()
                .filter(kh -> !"Đã hủy".equalsIgnoreCase(kh.getTrangThai()))
                .count();
        long daHuy = tatCa.stream()
                .filter(kh -> "Đã hủy".equalsIgnoreCase(kh.getTrangThai()))
                .count();

        model.addAttribute("listKH", khachHangService.timKiemVaLocKhachHang(keyword, trangThai));
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai != null ? trangThai : "ALL");
        model.addAttribute("tongKhach", tongKhach);
        model.addAttribute("dangHoatDong", dangHoatDong);
        model.addAttribute("daHuy", daHuy);
        model.addAttribute("logList", khachHangScheduledService.docLichSuLogTuFile());
        return "AdminKhachHang";
    }

    // =========================================================================
    // IV. REST API XEM CHI TIẾT & ĐỒNG BỘ LUỒNG @SCHEDULED
    // =========================================================================

    @GetMapping({"/khachhang/chitiet/{id}", "/admin/khachhang/chitiet/{id}"})
    @ResponseBody
    public ResponseEntity<?> xemChiTietKhachHang(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(khachHangService.layChiTietKhachHang(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping({"/khachhang/api/dongbo-luong", "/admin/khachhang/api/dongbo-luong"})
    @ResponseBody
    public ResponseEntity<?> dongBoLuongKhachHang() {
        khachHangScheduledService.tuDongQuetVaGhiLogKhachHang();
        return ResponseEntity.ok(Map.of(
                "message", "Đã thực thi quét định kỳ & ghi log ra file logs/khach_hang_scheduled.log",
                "logDetail", khachHangScheduledService.getLastExecutionSummary(),
                "logs", khachHangScheduledService.docLichSuLogTuFile()
        ));
    }
}
