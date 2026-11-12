package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/hoadon")
public class HoaDonController {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private RoomRepository roomRepository;

    // 1. Danh sách hóa đơn
    @GetMapping("/danhsach")
    public String danhsachHoaDon(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            Model model) {

        String keyword = (tuKhoa != null) ? tuKhoa.trim() : null;
        List<HoaDon> dsHoaDon = hoaDonRepository.timKiemHoaDon(keyword, trangThai);
        dsHoaDon.sort(java.util.Comparator.comparing(HoaDon::getMaHoaDon));

        model.addAttribute("danhSachHoaDon", dsHoaDon);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("tongDoanhThu", hoaDonRepository.tinhTongDoanhThu());
        return "HoaDonDanhSach";
    }

    // 2. Trang Check-out tính tiền theo số phòng
    @GetMapping("/checkout/{soPhong}")
    public String tinhTienCheckOut(@PathVariable("soPhong") String soPhong, Model model) {
        Room room = roomRepository.findById(soPhong).orElse(null);
        DatPhong datPhong = datPhongRepository
                .findFirstBySoPhongAndTrangThaiOrderByNgayNhanPhongDesc(soPhong, "Đang lưu trú")
                .orElse(null);

        if (room == null || datPhong == null) {
            return "redirect:/khachhang/phong";
        }

        LocalDateTime ngayNhan = datPhong.getNgayNhanPhong() != null
                ? datPhong.getNgayNhanPhong()
                : LocalDateTime.now().minusDays(1);
        LocalDateTime ngayTra = LocalDateTime.now();
        long soNgayO = Math.max(1, Duration.between(ngayNhan, ngayTra).toDays());

        double tienPhong = soNgayO * room.getGiaPhong();

        // Lấy tổng tiền dịch vụ thực tế từ Repository (nếu chưa có dịch vụ sẽ trả về 0)
        Double tienDichVuDb = hoaDonRepository.tinhTongTienDichVuTheoDatPhong(datPhong.getMaDatPhong());
        double tienDichVu = (tienDichVuDb != null) ? tienDichVuDb : 0.0;

        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaDatPhong(datPhong.getMaDatPhong());
        hoaDon.setDatPhong(datPhong);
        hoaDon.setTienPhong(tienPhong);
        hoaDon.setTienDichVu(tienDichVu);
        hoaDon.setTongTien(tienPhong + tienDichVu);
        hoaDon.setTrangThai("Chưa thanh toán");

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("soNgayO", soNgayO);
        model.addAttribute("donGiaPhong", room.getGiaPhong());
        return "HoaDon";
    }

    // 3. Xác nhận thanh toán (Giao dịch nguyên tố @Transactional)
    @PostMapping("/thanhtoan")
    @Transactional
    public String xacNhanThanhToan(@ModelAttribute("hoaDon") HoaDon hoaDon) {
        hoaDon.setNgayLap(LocalDateTime.now());
        hoaDon.setTrangThai("Đã thanh toán");
        HoaDon hoaDonDaLuu = hoaDonRepository.save(hoaDon);

        DatPhong datPhong = datPhongRepository.findById(hoaDon.getMaDatPhong()).orElse(null);
        if (datPhong != null) {
            datPhong.setTrangThai("Đã trả phòng");
            datPhong.setNgayTraPhong(LocalDateTime.now());
            datPhongRepository.save(datPhong);

            Room room = roomRepository.findById(datPhong.getSoPhong()).orElse(null);
            if (room != null) {
                room.setTrangThai("Còn trống");
                roomRepository.save(room);
            }
        }

        return "redirect:/hoadon/chitiet/" + hoaDonDaLuu.getMaHoaDon();
    }

    // 4. Xem biên lai chi tiết để in
    @GetMapping("/chitiet/{id}")
    public String chiTietHoaDon(@PathVariable("id") Long id, Model model) {
        HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);
        return "ChiTietHoaDon";
    }
    // 5. Kiểm tra tên lễ tân bằng Java trước khi cho phép in biên lai
    @PostMapping("/chitiet/{id}/in")
    public String xacNhanInHoaDon(@PathVariable("id") Long id,
                                  @RequestParam(value = "tenLeTan", required = false) String tenLeTan,
                                  Model model) {
        HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);

        // Kiểm tra bằng Java: Nếu chưa nhập tên lễ tân thì báo lỗi và không cho in
        if (tenLeTan == null || tenLeTan.trim().isEmpty()) {
            model.addAttribute("loiLeTan", "⚠️ Vui lòng nhập họ tên lễ tân trước khi in biên lai!");
            model.addAttribute("choPhepIn", false);
            return "ChiTietHoaDon";
        }

        // Hợp lệ -> giữ lại tên lễ tân và cho phép kích hoạt lệnh in
        model.addAttribute("tenLeTan", tenLeTan.trim());
        model.addAttribute("choPhepIn", true);
        return "ChiTietHoaDon";
    }
}