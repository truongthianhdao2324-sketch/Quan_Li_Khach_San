package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.example.quan_li_khach_san.service.RoomService;
import org.example.quan_li_khach_san.entity.TaiKhoan;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@Controller
@RequestMapping("/khachhang")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomService roomService;

//    private final List<String> danhSachLoaiPhong = Arrays.asList(
//            "Tiêu chuẩn", "Cao cấp", "Sang trọng", "VIP"
//    );
//
//    private final List<String> danhSachTrangThai = Arrays.asList(
//            "Còn trống", "Đã có khách", "Đang dọn phòng"
//    );

    private final List<String> danhSachTrangThai = List.of(
            "Còn trống",
            "Đã có khách",
            "Đang dọn phòng"
    );

    private boolean laAdmin(HttpSession session) {
        TaiKhoan user = (TaiKhoan) session.getAttribute("user");

        return user != null
                && user.getVaiTro() != null
                && "ADMIN".equalsIgnoreCase(user.getVaiTro().trim());
    }


    public RoomController(RoomRepository roomRepository, RoomService roomService) {
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    // Hiển thị danh sách, tìm kiếm và lọc phòng
    @GetMapping("/phong")
    public String danhSachPhong(
            @RequestParam(required = false) String soPhong,
            @RequestParam(required = false) String loaiPhong,
            @RequestParam(required = false) String trangThai,
            HttpSession session,
            Model model) {

        TaiKhoan user = (TaiKhoan) session.getAttribute("user");

        if (user == null) {
            return "redirect:/dang-nhap";
        }

        String vaiTro = user.getVaiTro() == null
                ? ""
                : user.getVaiTro().trim();

        boolean laAdmin = "ADMIN".equalsIgnoreCase(vaiTro);

        if (!laAdmin
                && !"RECEPTIONIST".equalsIgnoreCase(vaiTro)
                && !"letan".equalsIgnoreCase(vaiTro)) {
            return "redirect:/dang-nhap";
        }

        soPhong = (soPhong == null || soPhong.trim().isEmpty())
                ? null : soPhong.trim();

        loaiPhong = (loaiPhong == null || loaiPhong.isEmpty())
                ? null : loaiPhong;

        trangThai = (trangThai == null || trangThai.isEmpty())
                ? null : trangThai;

        List<Room> danhSachPhong =
                roomRepository.timKiemVaLoc(soPhong, loaiPhong, trangThai);

        model.addAttribute("danhSachPhong", danhSachPhong);
        model.addAttribute("danhSachLoaiPhong", roomRepository.findAllLoaiPhong());

        // Thống kê phòng
        model.addAttribute("tongSoPhong", roomRepository.count());

        model.addAttribute("thongKeTrangThai",
                roomRepository.thongKeTheoTrangThai());

        model.addAttribute("thongKeLoaiPhong",
                roomRepository.thongKeTheoLoaiPhong());

        model.addAttribute("laAdmin", laAdmin);
        model.addAttribute("soPhongTim", soPhong);
        model.addAttribute("loaiPhongChon", loaiPhong);
        model.addAttribute("trangThaiChon", trangThai);

        return "DanhSachPhong";
    }

    // Lễ tân và Admin cập nhật trạng thái phòng
    @PostMapping("/phong/doi-trang-thai")
    public String doiTrangThai(
            @RequestParam String soPhong,
            @RequestParam String trangThai,
            HttpSession session) {

        TaiKhoan user = (TaiKhoan) session.getAttribute("user");

        if (user == null) {
            return "redirect:/dang-nhap";
        }

        String vaiTro = user.getVaiTro() == null
                ? ""
                : user.getVaiTro().trim();

        if (!"ADMIN".equalsIgnoreCase(vaiTro)
                && !"RECEPTIONIST".equalsIgnoreCase(vaiTro)
                && !"letan".equalsIgnoreCase(vaiTro)) {
            return "redirect:/dang-nhap";
        }

        if (!List.of("Còn trống", "Đã có khách", "Đang dọn phòng")
                .contains(trangThai)) {
            return "redirect:/khachhang/phong";
        }

        String ketQua = roomService.doiTrangThai(soPhong, trangThai);

        return "redirect:/khachhang/phong?thongBao=" +
                java.net.URLEncoder.encode(
                        ketQua,
                        java.nio.charset.StandardCharsets.UTF_8);
    }



    // ================= THÊM PHÒNG =================
    @GetMapping("/phong/them")
    public String hienThiFormThem(HttpSession session, Model model) {

        if (!laAdmin(session)) {
            return "redirect:/khachhang/phong";
        }

        model.addAttribute("room", new Room());
        model.addAttribute("danhSachLoaiPhong", roomRepository.findAllLoaiPhong());
        model.addAttribute("danhSachTrangThai", danhSachTrangThai);

        return "ThemPhong";
    }

    @PostMapping("/phong/them")
    public String themPhong(
            @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!laAdmin(session)) {
            return "redirect:/khachhang/phong";
        }

        // Kiểm tra số phòng
        if (room.getSoPhong() == null || room.getSoPhong().trim().isEmpty()) {
            bindingResult.rejectValue("soPhong", "empty",
                    "Vui lòng nhập số phòng");
        } else {
            room.setSoPhong(room.getSoPhong().trim());

            if (room.getSoPhong().length() > 20) {
                bindingResult.rejectValue("soPhong", "length",
                        "Số phòng không được vượt quá 20 ký tự");
            } else if (roomRepository.existsById(room.getSoPhong())) {
                bindingResult.rejectValue("soPhong", "duplicate",
                        "Số phòng này đã tồn tại");
            }
        }

        // Kiểm tra giá phòng
        if (!bindingResult.hasFieldErrors("giaPhong")
                && (!Double.isFinite(room.getGiaPhong()) || room.getGiaPhong() <= 0)) {
            bindingResult.rejectValue("giaPhong", "invalid",
                    "Giá phòng phải lớn hơn 0");
        }

        // Kiểm tra loại phòng
        if (room.getLoaiPhong() == null
                || !roomRepository.findAllLoaiPhong().contains(room.getLoaiPhong())) {
            bindingResult.rejectValue("loaiPhong", "invalid",
                    "Vui lòng chọn loại phòng hợp lệ");
        }

        // Kiểm tra trạng thái
        if (room.getTrangThai() == null
                || !danhSachTrangThai.contains(room.getTrangThai())) {
            bindingResult.rejectValue("trangThai", "invalid",
                    "Vui lòng chọn trạng thái hợp lệ");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("danhSachLoaiPhong", roomRepository.findAllLoaiPhong());
            model.addAttribute("danhSachTrangThai", danhSachTrangThai);
            return "ThemPhong";
        }

        roomRepository.save(room);

        redirectAttributes.addFlashAttribute(
                "thongBao", "Thêm phòng thành công!");

        return "redirect:/khachhang/phong";
    }


// ================= SỬA PHÒNG =================

    @GetMapping("/phong/sua/{soPhong}")
    public String hienThiFormSua(
            @PathVariable String soPhong,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!laAdmin(session)) {
            return "redirect:/khachhang/phong";
        }

        Room phong = roomRepository.findById(soPhong).orElse(null);

        if (phong == null) {
            redirectAttributes.addFlashAttribute(
                    "loi", "Không tìm thấy phòng cần sửa!");
            return "redirect:/khachhang/phong";
        }

        model.addAttribute("room", phong);
        model.addAttribute("danhSachLoaiPhong", roomRepository.findAllLoaiPhong());

        return "SuaPhong";
    }

    @PostMapping("/phong/sua/{soPhong}")
    public String suaPhong(
            @PathVariable String soPhong,
            @ModelAttribute("room") Room room,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!laAdmin(session)) {
            return "redirect:/khachhang/phong";
        }

        Room phong = roomRepository.findById(soPhong).orElse(null);

        if (phong == null) {
            redirectAttributes.addFlashAttribute(
                    "loi", "Không tìm thấy phòng cần sửa!");
            return "redirect:/khachhang/phong";
        }

        // Kiểm tra giá phòng
        if (!bindingResult.hasFieldErrors("giaPhong")
                && (!Double.isFinite(room.getGiaPhong()) || room.getGiaPhong() <= 0)) {
            bindingResult.rejectValue("giaPhong", "invalid",
                    "Giá phòng phải lớn hơn 0");
        }

        // Kiểm tra loại phòng
        if (room.getLoaiPhong() == null
                || !roomRepository.findAllLoaiPhong().contains(room.getLoaiPhong())) {
            bindingResult.rejectValue("loaiPhong", "invalid",
                    "Vui lòng chọn loại phòng hợp lệ");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("danhSachLoaiPhong", roomRepository.findAllLoaiPhong());
            return "SuaPhong";
        }

        // Chỉ cập nhật giá và loại phòng
        phong.setGiaPhong(room.getGiaPhong());
        phong.setLoaiPhong(room.getLoaiPhong());

        roomRepository.save(phong);

        redirectAttributes.addFlashAttribute(
                "thongBao", "Cập nhật phòng thành công!");

        return "redirect:/khachhang/phong";
    }


// ================= XÓA PHÒNG =================

    @PostMapping("/phong/xoa/{soPhong}")
    public String xoaPhong(
            @PathVariable String soPhong,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!laAdmin(session)) {
            return "redirect:/khachhang/phong";
        }

        if (!roomRepository.existsById(soPhong)) {
            redirectAttributes.addFlashAttribute(
                    "loi", "Không tìm thấy phòng cần xóa!");
            return "redirect:/khachhang/phong";
        }

        try {
            roomRepository.deleteById(soPhong);
            roomRepository.flush();

            redirectAttributes.addFlashAttribute(
                    "thongBao", "Xóa phòng thành công!");

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute(
                    "loi", "Không thể xóa phòng vì đã có dữ liệu đặt phòng liên quan!");
        }

        return "redirect:/khachhang/phong";
    }
}