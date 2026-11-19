package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.Employee;
import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.example.quan_li_khach_san.repository.EmployeeRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class KhachHangController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // --- QUẢN LÝ KHÁCH HÀNG ---

    @GetMapping("/khachhang/danhsach")
    public String hienThiDanhSach(@RequestParam(value = "tuKhoa", required = false) String tuKhoa,
                                  HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        List<KhachHang> danhSach;
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            danhSach = khachHangRepository.findByHoTenContainingOrSoGiayToContaining(tuKhoa, tuKhoa);
        } else {
            danhSach = khachHangRepository.findAll();
        }

        model.addAttribute("danhSachKhachHang", danhSach);
        model.addAttribute("tuKhoa", tuKhoa);
        return "DanhSachKH";
    }

    @GetMapping("/khachhang/form-them")
    public String hienThiFormThem(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        model.addAttribute("khachHang", new KhachHang());
        model.addAttribute("danhSachPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        return "ThemKhachHang";
    }

    @PostMapping("/khachhang/luu")
    @Transactional
    public String luuKhachHang(@ModelAttribute("khachHang") KhachHang khachHang, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        khachHangRepository.save(khachHang);
        if (khachHang.getSoPhong() != null && !khachHang.getSoPhong().isEmpty()) {
            roomRepository.updateTrangThai(khachHang.getSoPhong(), "Đã có khách");
        }
        return "redirect:/khachhang/danhsach";
    }

    @GetMapping("/khachhang/xoa")
    @Transactional
    public String xoaKhachHang(@RequestParam("soGiayTo") String soGiayTo, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        khachHangRepository.deleteBySoGiayTo(soGiayTo);
        return "redirect:/khachhang/danhsach";
    }

    @GetMapping("/khachhang/form-sua")
    public String hienThiFormSua(@RequestParam("soGiayTo") String soGiayTo, HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        KhachHang khachHang = khachHangRepository.findBySoGiayTo(soGiayTo).orElse(null);
        model.addAttribute("khachHang", khachHang);
        model.addAttribute("danhSachPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        return "SuaKhachHang";
    }

    @PostMapping("/khachhang/cap-nhat")
    @Transactional
    public String capNhatKhachHang(@ModelAttribute("khachHang") KhachHang khachHang, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        khachHangRepository.save(khachHang);
        if (khachHang.getSoPhong() != null && !khachHang.getSoPhong().isEmpty()) {
            roomRepository.updateTrangThai(khachHang.getSoPhong(), "Đã có khách");
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

    @GetMapping("/khachhang/phong/form-them")
    public String hienThiFormThemPhong(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        model.addAttribute("room", new Room());
        return "ThemPhong";
    }

    @PostMapping("/khachhang/phong/luu")
    public String luuPhongMoi(@ModelAttribute("room") Room room, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        roomRepository.save(room);
        return "redirect:/khachhang/phong";
    }

    @GetMapping("/khachhang/phong/doi-trang-thai")
    @Transactional
    public String doiTrangThaiPhong(@RequestParam("soPhong") String soPhong, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null) return "redirect:/dang-nhap";

        roomRepository.findById(soPhong).ifPresent(room -> {
            String trangThaiHienTai = room.getTrangThai();
            String trangThaiMoi = "Còn trống";

            if ("Còn trống".equals(trangThaiHienTai)) {
                trangThaiMoi = "Đã có khách";
            } else if ("Đã có khách".equals(trangThaiHienTai)) {
                trangThaiMoi = "Đang dọn phòng";
            } else if ("Đang dọn phòng".equals(trangThaiHienTai)) {
                trangThaiMoi = "Còn trống";
            }

            room.setTrangThai(trangThaiMoi);
            roomRepository.save(room);
        });
        return "redirect:/khachhang/phong";
    }

    // --- ADMIN QUẢN LÝ NHÂN SỰ ---

    @GetMapping("/admin/nhanvien")
    public String adminHienThiDanhSachNhanVien(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("danhSachNhanVien", employeeRepository.findAll());
        return "DanhSachNhanVien";
    }

    @GetMapping("/admin/nhanvien/form-them")
    public String adminHienThiFormThemNhanVien(HttpSession session, Model model) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("employee", new Employee());
        return "ThemNhanVien";
    }

    @PostMapping("/admin/nhanvien/luu")
    public String adminLuuNhanVienMoi(@ModelAttribute("employee") Employee employee, HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getVaiTro())) {
            return "redirect:/dang-nhap";
        }

        employeeRepository.save(employee);
        return "redirect:/admin/nhanvien";
    }
}