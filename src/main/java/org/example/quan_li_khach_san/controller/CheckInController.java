package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
<<<<<<< HEAD
import org.springframework.transaction.annotation.Transactional;
=======
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CheckInController {

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private RoomRepository roomRepository;

    // 1. Mở giao diện Check-in và truyền dữ liệu danh sách phòng trống, khách hàng sang view
    @GetMapping("/checkin")
<<<<<<< HEAD
    public String hienThiTrangCheckIn(
            @RequestParam(name = "soPhong", required = false) String soPhong,
            Model model) {
        model.addAttribute("listPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        model.addAttribute("listKhachHang", khachHangRepository.findAll());
        model.addAttribute("soPhongDaChon", soPhong);
        model.addAttribute("datPhong", new DatPhong());
        return "CheckIn";
    }

    // 2. Xử lý lưu thông tin nhận phòng khi lễ tân bấm nút xác nhận
    @PostMapping("/checkin/save")
    @Transactional
    public String thucHienCheckIn(@ModelAttribute("datPhong") DatPhong datPhong) {
        String soPhong = datPhong.getSoPhong();
        if (soPhong == null || soPhong.isBlank()) {
            return "redirect:/checkin?error=phong";
        }

        datPhong.setSoPhong(soPhong);

        if (datPhong.getMaPhong() == null) {
            String digits = soPhong.replaceAll("\\D+", "");
            if (!digits.isEmpty()) {
                datPhong.setMaPhong(Integer.parseInt(digits));
            } else {
                datPhong.setMaPhong(0);
            }
        }

        datPhong.setTrangThai("Đã có khách");
        datPhongRepository.save(datPhong);

        roomRepository.updateTrangThai(soPhong, "Đã có khách");
=======
    public String hienThiTrangCheckIn(Model model) {
        model.addAttribute("listPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        model.addAttribute("listKhachHang", khachHangRepository.findAll());
        model.addAttribute("datPhong", new DatPhong()); // <--- Khởi tạo mới hoàn toàn trống (các thuộc tính Integer bên trong sẽ là null)
        return "CheckIn";
    }
    // 2. Xử lý lưu thông tin nhận phòng khi lễ tân bấm nút xác nhận
    @PostMapping("/checkin/save")
    public String thucHienCheckIn(@ModelAttribute("datPhong") DatPhong datPhong) {
        // Cập nhật trạng thái đặt phòng
        datPhong.setTrangThai("Dang su dung");
        datPhongRepository.save(datPhong);

        // Cập nhật trạng thái phòng thành "Đang sử dụng" hoặc tùy chỉnh theo database của nhóm
        roomRepository.updateTrangThai(String.valueOf(datPhong.getMaPhong()), "Dang su dung");
>>>>>>> 6026e3ee81e4d8efd379d4baf1aa115026e76d3e

        return "redirect:/checkin?success";
    }
}