package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
    public String hienThiTrangCheckIn(Model model) {
        model.addAttribute("listPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        model.addAttribute("listKhachHang", khachHangRepository.findAll());
        model.addAttribute("datPhong", new DatPhong());
        return "CheckIn"; // Trỏ đến file CheckIn.html trong thư mục templates
    }

    // 2. Xử lý lưu thông tin nhận phòng khi lễ tân bấm nút xác nhận
    @PostMapping("/checkin/save")
    public String thucHienCheckIn(@ModelAttribute("datPhong") DatPhong datPhong) {
        // Cập nhật trạng thái đặt phòng
        datPhong.setTrangThai("Dang su dung");
        datPhongRepository.save(datPhong);

        // Cập nhật trạng thái phòng thành "Đang sử dụng" hoặc tùy chỉnh theo database của nhóm
        roomRepository.updateTrangThai(String.valueOf(datPhong.getMaPhong()), "Dang su dung");

        return "redirect:/checkin?success";
    }
}