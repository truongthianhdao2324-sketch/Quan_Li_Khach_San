package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.quan_li_khach_san.entity.*;
import org.example.quan_li_khach_san.service.HoaDonNetworkService;
import org.example.quan_li_khach_san.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Controller
public class HoaDonController {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private DatPhongRepository datPhongRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HoaDonNetworkService hoaDonNetworkService;

    // 1. Danh sách hóa đơn (Kênh Lễ tân)
    @GetMapping("/hoadon/danhsach")
    public String danhsachHoaDon(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            Model model) {

        String keyword = (tuKhoa != null) ? tuKhoa.trim() : null;
        List<HoaDon> dsHoaDon = hoaDonRepository.timKiemHoaDon(keyword, trangThai);
        dsHoaDon.sort(Comparator.comparing(HoaDon::getMaHoaDon));

        model.addAttribute("danhSachHoaDon", dsHoaDon);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("tongDoanhThu", hoaDonRepository.tinhTongDoanhThu());
        return "HoaDonDanhSach";
    }

    // 2. Trang Check-out tính tiền theo số phòng
    @GetMapping("/hoadon/checkout/{soPhong}")
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
    @PostMapping("/hoadon/thanhtoan")
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
    @GetMapping("/hoadon/chitiet/{id}")
    public String chiTietHoaDon(@PathVariable("id") Long id, Model model) {
        HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);
        return "ChiTietHoaDon";
    }

    // 5. Kiểm tra tên lễ tân bằng Java trước khi cho phép in biên lai
    @PostMapping("/hoadon/chitiet/{id}/in")
    public String xacNhanInHoaDon(@PathVariable("id") Long id,
                                  @RequestParam(value = "tenLeTan", required = false) String tenLeTan,
                                  Model model) {
        HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);

        if (tenLeTan == null || tenLeTan.trim().isEmpty()) {
            model.addAttribute("loiLeTan", "⚠️ Vui lòng nhập họ tên lễ tân trước khi in biên lai!");
            model.addAttribute("choPhepIn", false);
            return "ChiTietHoaDon";
        }

        model.addAttribute("tenLeTan", tenLeTan.trim());
        model.addAttribute("choPhepIn", true);
        return "ChiTietHoaDon";
    }

    // =========================================================================
    // 6. KÊNH ADMIN: ĐỒNG BỘ HÓA ĐƠN QUA URLCONNECTION (CHƯƠNG 5)
    // =========================================================================
    @GetMapping("/admin/hoadon")
    public String trangHoaDonAdmin(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            Model model) {

        HoaDonNetworkService.KetQuaDongBo kq = hoaDonNetworkService.dongBoTuServer(tuKhoa, trangThai);

        model.addAttribute("danhSachDongBo", kq.danhSachHoaDon);
        model.addAttribute("tongDoanhThu", kq.tongDoanhThu);
        model.addAttribute("tongHoaDon", kq.soDaThanhToan + kq.soChuaThanhToan);
        model.addAttribute("soDaThanhToan", kq.soDaThanhToan);
        model.addAttribute("soChuaThanhToan", kq.soChuaThanhToan);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai != null ? trangThai : "");
        model.addAttribute("ketNoiThanhCong", kq.ketNoiThanhCong);
        model.addAttribute("thoiGianCapNhat", kq.thoiGianCapNhat);
        model.addAttribute("nhatKyMang", kq.nhatKyMang);

        return "AdminHoaDon";
    }

    // 7. Xuất báo cáo CSV trực tiếp bằng Java
    @GetMapping("/admin/hoadon/xuat-csv")
    public void xuatBaoCaoCsvAdmin(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            HttpServletResponse response) throws Exception {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "BaoCaoHoaDon_Admin_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        HoaDonNetworkService.KetQuaDongBo kq = hoaDonNetworkService.dongBoTuServer(tuKhoa, trangThai);

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            writer.println("MaHD,SoPhong,KhachHang,NgayLap,TienPhong,TienDichVu,TongTien,TrangThai");

            for (String[] row : kq.danhSachHoaDon) {
                writer.printf("%s,%s,\"%s\",%s,%.0f,%.0f,%.0f,\"%s\"%n",
                        row[1],
                        row[2],
                        row[3].replace("\"", "\"\""),
                        row[4],
                        Double.parseDouble(row[5]),
                        Double.parseDouble(row[6]),
                        Double.parseDouble(row[7]),
                        row[8]);
            }
        }
    }
}
