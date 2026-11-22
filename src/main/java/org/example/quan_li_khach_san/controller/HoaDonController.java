package org.example.quan_li_khach_san.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.quan_li_khach_san.entity.HoaDon;
import org.example.quan_li_khach_san.entity.TaiKhoan;
import org.example.quan_li_khach_san.service.HoaDonLogService;
import org.example.quan_li_khach_san.service.HoaDonNetworkService;
import org.example.quan_li_khach_san.service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private HoaDonNetworkService hoaDonNetworkService;

    @Autowired
    private HoaDonLogService hoaDonLogService;

    // Lấy tên tài khoản đang đăng nhập từ Session
    private String layTaiKhoanDangNhap(HttpSession session, String macDinh) {
        if (session != null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof TaiKhoan tk && tk.getTenDangNhap() != null && !tk.getTenDangNhap().isBlank()) {
                return tk.getTenDangNhap().trim();
            }
        }
        return macDinh;
    }

    // =========================================================================
    // 1. KÊNH LỄ TÂN: XEM DANH SÁCH HÓA ĐƠN
    // =========================================================================
    @GetMapping({"/hoadon/danhsach", "/letan/hoadon"})
    public String danhsachHoaDon(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            Model model) {

        List<HoaDon> dsHoaDon = hoaDonService.timKiemVaSapXep(tuKhoa, trangThai);

        model.addAttribute("danhSachHoaDon", dsHoaDon);
        model.addAttribute("mapGiaPhong", hoaDonService.layMapGiaPhong(dsHoaDon));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        return "HoaDonDanhSach";
    }

    // =========================================================================
    // 2. KÊNH LỄ TÂN: MỞ TRANG CHỌN PHÒNG CHECK-OUT RIÊNG BIỆT
    // =========================================================================
    @GetMapping("/hoadon/checkout")
    public String trangChonPhongCheckOut(Model model) {
        model.addAttribute("dsPhongDangO", hoaDonService.layDanhSachPhongDangLuuTru());
        return "check-out";
    }

    // =========================================================================
    // 3. KÊNH LỄ TÂN: MỞ TRANG TẠM TÍNH TIỀN CỦA PHÒNG ĐƯỢC CHỌN
    // =========================================================================
    @GetMapping("/hoadon/checkout/{soPhong}")
    public String tinhTienCheckOut(@PathVariable("soPhong") String soPhong, Model model) {
        HoaDonService.ThongTinCheckOut kq = hoaDonService.chuanBiCheckOut(soPhong);

        if (kq == null) {
            return "redirect:/hoadon/checkout";
        }

        model.addAttribute("hoaDon", kq.hoaDon);
        model.addAttribute("soNgayO", kq.soNgayO);
        model.addAttribute("donGiaPhong", kq.donGiaPhong);
        return "HoaDon";
    }

    // =========================================================================
    // 4. KÊNH LỄ TÂN: XÁC NHẬN THANH TOÁN & HOÀN TẤT TRẢ PHÒNG
    // =========================================================================
    @PostMapping("/hoadon/thanhtoan")
    public String xacNhanThanhToan(@ModelAttribute("hoaDon") HoaDon hoaDon, HttpSession session) {
        String taiKhoan = layTaiKhoanDangNhap(session, "letan");
        HoaDon hoaDonDaLuu = hoaDonService.xacNhanThanhToanVaTraPhong(hoaDon.getMaDatPhong(), taiKhoan);

        if (hoaDonDaLuu == null) {
            return "redirect:/hoadon/checkout";
        }

        return "redirect:/hoadon/chitiet/" + hoaDonDaLuu.getMaHoaDon();
    }

    // =========================================================================
    // 5. KÊNH LỄ TÂN: XEM CHI TIẾT BIÊN LAI THANH TOÁN
    // =========================================================================
    @GetMapping("/hoadon/chitiet/{id}")
    public String chiTietHoaDon(@PathVariable("id") Long id, Model model) {
        HoaDon hoaDon = hoaDonService.layTheoId(id);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("soNgayO", hoaDonService.tinhSoNgayOChoHoaDon(hoaDon));
        model.addAttribute("donGiaPhong", hoaDonService.layDonGiaPhong(hoaDon));
        return "ChiTietHoaDon";
    }

    // =========================================================================
    // 6. KÊNH LỄ TÂN: KIỂM TRA TÊN LỄ TÂN TRƯỚC KHI IN BIÊN LAI
    // =========================================================================
    @PostMapping("/hoadon/chitiet/{id}/in")
    public String xacNhanInHoaDon(@PathVariable("id") Long id,
                                  @RequestParam(value = "tenLeTan", required = false) String tenLeTan,
                                  HttpSession session,
                                  Model model) {
        HoaDon hoaDon = hoaDonService.layTheoId(id);
        if (hoaDon == null) return "redirect:/hoadon/danhsach";

        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("soNgayO", hoaDonService.tinhSoNgayOChoHoaDon(hoaDon));
        model.addAttribute("donGiaPhong", hoaDonService.layDonGiaPhong(hoaDon));

        String taiKhoan = layTaiKhoanDangNhap(session, "letan");
        String tenKhach = (hoaDon.getDatPhong() != null && hoaDon.getDatPhong().getKhachHang() != null)
                ? hoaDon.getDatPhong().getKhachHang().getHoTen() : "Khách lẻ";
        String soPhong = (hoaDon.getDatPhong() != null) ? hoaDon.getDatPhong().getSoPhong() : "N/A";

        if (tenLeTan == null || tenLeTan.trim().isEmpty()) {
            hoaDonLogService.ghiLog(
                    taiKhoan,
                    "In biên lai thanh toán",
                    "Hóa đơn " + hoaDon.getMaHienThi() + " - Phòng " + soPhong + " - Khách hàng: " + tenKhach,
                    hoaDon.getTrangThai(),
                    hoaDon.getTrangThai(),
                    "Thất bại - Nhân viên chưa nhập họ tên ký xác nhận"
            );
            model.addAttribute("loiLeTan", "⚠️ Vui lòng nhập họ tên lễ tân trước khi in biên lai!");
            model.addAttribute("choPhepIn", false);
            return "ChiTietHoaDon";
        }

        hoaDonLogService.ghiLog(
                taiKhoan,
                "In biên lai thanh toán",
                "Hóa đơn " + hoaDon.getMaHienThi() + " - Phòng " + soPhong
                        + " - Khách hàng: " + tenKhach + " (Người ký: " + tenLeTan.trim() + ")",
                hoaDon.getTrangThai(),
                "Đã xuất in biên lai",
                "Thành công"
        );

        model.addAttribute("tenLeTan", tenLeTan.trim());
        model.addAttribute("choPhepIn", true);
        return "ChiTietHoaDon";
    }

    // =========================================================================
    // 7. KÊNH ADMIN: QUẢN LÝ HÓA ĐƠN & ĐỒNG BỘ DOANH THU QUA MẠNG (URLConnection)
    // =========================================================================
    @GetMapping("/admin/hoadon")
    public String trangHoaDonAdmin(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            HttpSession session,
            Model model) {

        String taiKhoan = layTaiKhoanDangNhap(session, "admin");
        HoaDonNetworkService.KetQuaDongBo kq = hoaDonNetworkService.dongBoTuServer(tuKhoa, trangThai, taiKhoan);

        model.addAttribute("danhSachDongBo", kq.danhSachHoaDon);
        model.addAttribute("tongDoanhThu", kq.tongDoanhThu);
        model.addAttribute("tongHoaDon", kq.soDaThanhToan + kq.soChuaThanhToan);
        model.addAttribute("soDaThanhToan", kq.soDaThanhToan);
        model.addAttribute("soChuaThanhToan", kq.soChuaThanhToan);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai != null ? trangThai : "");
        model.addAttribute("ketNoiThanhCong", kq.ketNoiThanhCong);
        model.addAttribute("thoiGianCapNhat", kq.thoiGianCapNhat);
        model.addAttribute("nhatKyHoaDon", hoaDonLogService.docLog());

        return "AdminHoaDon";
    }

    // =========================================================================
    // 8. KÊNH ADMIN: XUẤT BÁO CÁO HÓA ĐƠN RA FILE CSV (EXCEL)
    // =========================================================================
    @GetMapping("/admin/hoadon/xuat-csv")
    public void xuatBaoCaoCsvAdmin(
            @RequestParam(value = "tuKhoa", required = false) String tuKhoa,
            @RequestParam(value = "trangThai", required = false) String trangThai,
            HttpSession session,
            HttpServletResponse response) throws Exception {

        String taiKhoan = layTaiKhoanDangNhap(session, "admin");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "BaoCaoHoaDon_Admin_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        HoaDonNetworkService.KetQuaDongBo kq = hoaDonNetworkService.dongBoTuServer(tuKhoa, trangThai, taiKhoan);

        hoaDonLogService.ghiLog(
                taiKhoan,
                "Xuất báo cáo doanh thu CSV",
                "Tệp " + fileName + " (" + kq.danhSachHoaDon.size() + " hóa đơn)",
                "Dữ liệu hệ thống",
                "Đã tải xuống CSV",
                "Thành công"
        );

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