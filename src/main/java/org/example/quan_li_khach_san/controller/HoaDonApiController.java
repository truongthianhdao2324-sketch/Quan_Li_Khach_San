package org.example.quan_li_khach_san.controller;

import org.example.quan_li_khach_san.entity.HoaDon;
import org.example.quan_li_khach_san.repository.HoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/hoadon")
public class HoaDonApiController {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // Endpoint phục vụ Client Kế toán kết nối qua URLConnection để đồng bộ dữ liệu
    @GetMapping(value = "/dong-bo", produces = "text/plain;charset=UTF-8")
    public String dongBoDuLieuHoaDon(
            @RequestParam(value = "trangThai", required = false) String trangThai) {

        List<HoaDon> dsHoaDon = hoaDonRepository.timKiemHoaDon(null, trangThai);
        dsHoaDon.sort(Comparator.comparing(HoaDon::getMaHoaDon));

        Double tongDoanhThu = hoaDonRepository.tinhTongDoanhThu();
        long soDaThanhToan = hoaDonRepository.countByTrangThai("Đã thanh toán");
        long soChuaThanhToan = hoaDonRepository.countByTrangThai("Chưa thanh toán");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder sb = new StringBuilder();

        // Dòng 1: Thông tin tổng hợp doanh thu
        sb.append("SUMMARY|")
                .append(tongDoanhThu != null ? tongDoanhThu : 0.0).append("|")
                .append(dsHoaDon.size()).append("|")
                .append(soDaThanhToan).append("|")
                .append(soChuaThanhToan).append("\n");

        // Các dòng tiếp theo: Chi tiết từng hóa đơn
        for (HoaDon hd : dsHoaDon) {
            String soPhong = (hd.getDatPhong() != null && hd.getDatPhong().getSoPhong() != null)
                    ? hd.getDatPhong().getSoPhong() : "N/A";

            String tenKhach = (hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null)
                    ? hd.getDatPhong().getKhachHang().getHoTen() : "Khách lẻ";

            String ngayLap = (hd.getNgayLap() != null)
                    ? hd.getNgayLap().format(fmt) : "Chưa lập";

            sb.append("ROW|")
                    .append(hd.getMaHienThi()).append("|")
                    .append(soPhong).append("|")
                    .append(tenKhach).append("|")
                    .append(ngayLap).append("|")
                    .append(hd.getTienPhong()).append("|")
                    .append(hd.getTienDichVu()).append("|")
                    .append(hd.getTongTien()).append("|")
                    .append(hd.getTrangThai() != null ? hd.getTrangThai() : "Chưa rõ")
                    .append("\n");
        }

        return sb.toString();
    }
}