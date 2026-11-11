package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.Room;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.example.quan_li_khach_san.service.RoomService;
import org.example.quan_li_khach_san.entity.TaiKhoan;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/khachhang")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomService roomService;

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
}