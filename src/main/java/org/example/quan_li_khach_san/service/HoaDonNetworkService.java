package org.example.quan_li_khach_san.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HoaDonNetworkService {

    private static final String SERVER_API_URL = "http://localhost:8080/api/hoadon/dong-bo";

    @Autowired
    private HoaDonLogService hoaDonLogService;

    public static class KetQuaDongBo {
        public List<String[]> danhSachHoaDon = new ArrayList<>();
        public double tongDoanhThu = 0.0;
        public long soDaThanhToan = 0;
        public long soChuaThanhToan = 0;
        public boolean ketNoiThanhCong = false;
        public String thoiGianCapNhat = "";
    }

    public KetQuaDongBo dongBoTuServer(String tuKhoa, String trangThai, String nguoiThucHien) {
        KetQuaDongBo ketQua = new KetQuaDongBo();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        ketQua.thoiGianCapNhat = LocalDateTime.now().format(timeFmt);

        try {
            String fullUrl = SERVER_API_URL;
            if (trangThai != null && !trangThai.trim().isEmpty()) {
                fullUrl += "?trangThai=" + URLEncoder.encode(trangThai.trim(), StandardCharsets.UTF_8);
            }

            URL url = new URL(fullUrl);
            long start = System.currentTimeMillis();

            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            String keyword = (tuKhoa != null) ? tuKhoa.trim().toLowerCase() : "";

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    if (inputLine.startsWith("SUMMARY|")) {
                        String[] sumParts = inputLine.split("\\|");
                        if (sumParts.length >= 5) {
                            ketQua.tongDoanhThu = Double.parseDouble(sumParts[1]);
                            ketQua.soDaThanhToan = Long.parseLong(sumParts[3]);
                            ketQua.soChuaThanhToan = Long.parseLong(sumParts[4]);
                        }
                    } else if (inputLine.startsWith("ROW|")) {
                        String[] row = inputLine.split("\\|");
                        if (row.length >= 9) {
                            boolean khopTuKhoa = keyword.isEmpty()
                                    || row[1].toLowerCase().contains(keyword)
                                    || row[2].toLowerCase().contains(keyword)
                                    || row[3].toLowerCase().contains(keyword);
                            if (khopTuKhoa) {
                                ketQua.danhSachHoaDon.add(row);
                            }
                        }
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            ketQua.ketNoiThanhCong = true;

            hoaDonLogService.ghiLog(
                    nguoiThucHien,
                    "Đồng bộ danh sách hóa đơn từ Server (URLConnection)",
                    "Tải về " + ketQua.danhSachHoaDon.size() + " hóa đơn (Tổng doanh thu: "
                            + String.format("%,.0f", ketQua.tongDoanhThu) + " đ)",
                    "Dữ liệu cũ",
                    "Đã đồng bộ mới",
                    "Thành công (" + elapsed + "ms)"
            );

        } catch (Exception e) {
            hoaDonLogService.ghiLog(
                    nguoiThucHien,
                    "Đồng bộ danh sách hóa đơn từ Server (URLConnection)",
                    "Kết nối tới " + SERVER_API_URL,
                    "Chưa đồng bộ",
                    "Mất kết nối",
                    "Thất bại - " + e.getMessage()
            );
        }

        return ketQua;
    }
}