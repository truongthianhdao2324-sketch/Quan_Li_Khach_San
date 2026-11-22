package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.repository.DatPhongRepository;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.example.quan_li_khach_san.repository.RoomRepository;
import org.example.quan_li_khach_san.service.KhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CheckInController {

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private KhachHangService khachHangService;

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("/checkin")
    public String hienThiTrangCheckIn(
            @RequestParam(name = "soPhong", required = false) String soPhong,
            Model model) {
        DatPhong datPhong = new DatPhong();
        datPhong.setKhachHang(new KhachHang());
        datPhong.setNgayNhanPhong(LocalDateTime.now().withSecond(0).withNano(0));

        // Chỉ hiển thị các khách hàng đang hoạt động (loại bỏ khách hàng "Đã hủy")
        List<KhachHang> dsKhachHopLe = khachHangService.layTatCaKhachHang().stream()
                .filter(kh -> !"Đã hủy".equalsIgnoreCase(kh.getTrangThai()))
                .collect(Collectors.toList());

        model.addAttribute("listPhongTrong", roomRepository.findSoPhongByTrangThaiTrong());
        model.addAttribute("listKhachHang", dsKhachHopLe);
        model.addAttribute("soPhongDaChon", soPhong);
        model.addAttribute("datPhong", datPhong);
        return "CheckIn";
    }

    @PostMapping("/checkin/save")
    @Transactional
    public String thucHienCheckIn(
            @ModelAttribute("datPhong") DatPhong datPhong,
            @RequestParam(name = "maKhachHang", required = false) Long maKhachHangParam,
            RedirectAttributes redirectAttributes) {

        String soPhong = datPhong.getSoPhong();
        if (soPhong == null || soPhong.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn phòng còn trống!");
            return "redirect:/checkin";
        }

        Long maKh = (datPhong.getKhachHang() != null && datPhong.getKhachHang().getMaKhachHang() != null)
                ? datPhong.getKhachHang().getMaKhachHang()
                : maKhachHangParam;

        if (maKh == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn khách hàng nhận phòng!");
            return "redirect:/checkin";
        }

        KhachHang khachHang = khachHangRepository.findById(maKh).orElse(null);
        if (khachHang == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng!");
            return "redirect:/checkin";
        }

        // Kiểm tra trạng thái hủy của khách hàng
        khachHangService.apDungTrangThaiHuy(khachHang);
        if ("Đã hủy".equalsIgnoreCase(khachHang.getTrangThai())) {
            redirectAttributes.addFlashAttribute("error",
                    "Khách hàng " + khachHang.getMaHienThi() + " (" + khachHang.getHoTen() + ") đã bị hủy hồ sơ, không thể nhận phòng!");
            return "redirect:/checkin";
        }

        if (datPhong.getNgayNhanPhong() == null) {
            datPhong.setNgayNhanPhong(LocalDateTime.now());
        }

        // Ràng buộc ngày trả phòng phải lớn hơn ngày nhận phòng
        if (datPhong.getNgayTraPhong() != null
                && !datPhong.getNgayTraPhong().isAfter(datPhong.getNgayNhanPhong())) {
            redirectAttributes.addFlashAttribute("error", "Ngày trả phòng dự kiến phải lớn hơn ngày nhận phòng!");
            return "redirect:/checkin";
        }

        datPhong.setKhachHang(khachHang);
        datPhong.setSoPhong(soPhong.trim());
        datPhong.setTrangThai("Đang lưu trú");
        datPhongRepository.save(datPhong);

        // Cập nhật phòng hiện tại cho khách hàng & trạng thái phòng sang "Đã có khách"
        khachHang.setSoPhong(soPhong.trim());
        khachHangRepository.save(khachHang);

        roomRepository.updateTrangThai(soPhong.trim(), "Đã có khách");

        return "redirect:/checkin?success";
    }
}