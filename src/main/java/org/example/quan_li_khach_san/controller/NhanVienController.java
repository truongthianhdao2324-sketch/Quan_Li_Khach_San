package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.Employee;
import org.example.quan_li_khach_san.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/nhanvien")
public class NhanVienController {

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. Hiển thị danh sách nhân sự (/admin/nhanvien)
    @GetMapping
    public String danhSachNhanVien(Model model) {
        model.addAttribute("danhSachNhanVien", employeeRepository.findAll());
        return "DanhSachNhanVien";
    }

    // 2. Mở form thêm nhân viên mới (/admin/nhanvien/form-them)
    @GetMapping("/form-them")
    public String formThemNhanVien(Model model) {
        model.addAttribute("nhanVien", new Employee());
        return "ThemNhanVien";
    }

    // 3. Lưu nhân viên mới vào database (/admin/nhanvien/them)
    @PostMapping("/them")
    public String luuNhanVien(@ModelAttribute Employee nhanVien) {
        employeeRepository.save(nhanVien);
        return "redirect:/admin/nhanvien";
    }
}