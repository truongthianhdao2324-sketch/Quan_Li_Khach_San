package org.example.quan_li_khach_san;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeToanClientApp {

    private static final String BASE_URL = "http://localhost:8080/api/hoadon/dong-bo";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile double doanhThuCu = -1;
    private static final Map<String, String> chuaThanhToanCu = new LinkedHashMap<>();
    private static final Map<String, String> daThanhToanCu = new LinkedHashMap<>();

    public static void main(String[] args) {
        int role = 0; // 0 = chạy cả 3 luồng trong 1 cửa sổ; 1 = Client-1; 2 = Client-2; 3 = Client-3
        if (args != null && args.length > 0) {
            try {
                role = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        ScheduledExecutorService pool = Executors.newScheduledThreadPool(role == 0 ? 3 : 1);

        switch (role) {
            case 1 -> {
                System.out.println("============================================================");
                System.out.println(" [CLIENT-1 | TỔNG HỢP DOANH THU] Đang chạy tự động (2s/lần)");
                System.out.println("============================================================");
                pool.scheduleAtFixedRate(KeToanClientApp::runClient1DoanhThu, 0, 2, TimeUnit.SECONDS);
            }
            case 2 -> {
                System.out.println("============================================================");
                System.out.println(" [CLIENT-2 | CHƯA THANH TOÁN] Đang chạy tự động (2s/lần)");
                System.out.println(" Tự động phát hiện thanh toán trên Web & báo Client-1, Client-3");
                System.out.println("============================================================");
                pool.scheduleAtFixedRate(KeToanClientApp::runClient2ChuaThanhToan, 0, 2, TimeUnit.SECONDS);
            }
            case 3 -> {
                System.out.println("============================================================");
                System.out.println(" [CLIENT-3 | ĐÃ THANH TOÁN] Đang chạy tự động (2s/lần)");
                System.out.println(" Tự động tiếp nhận thông báo & ghi nhận chốt sổ");
                System.out.println("============================================================");
                pool.scheduleAtFixedRate(KeToanClientApp::runClient3DaThanhToan, 0, 2, TimeUnit.SECONDS);
            }
            default -> {
                System.out.println("============================================================");
                System.out.println(" [ALL-IN-ONE] Chạy cả 3 Client tự động trong 1 tab");

                System.out.println("============================================================");
                pool.scheduleAtFixedRate(KeToanClientApp::runClient1DoanhThu, 0, 2, TimeUnit.SECONDS);
                pool.scheduleAtFixedRate(KeToanClientApp::runClient2ChuaThanhToan, 0, 2, TimeUnit.SECONDS);
                pool.scheduleAtFixedRate(KeToanClientApp::runClient3DaThanhToan, 0, 2, TimeUnit.SECONDS);
            }
        }
    }

    // CLIENT-1: Giám sát tổng doanh thu & nhận báo cáo khi có thanh toán mới
    private static void runClient1DoanhThu() {
        String body = fetchApi(null);
        if (body == null) return;

        double tongMoi = parseTongDoanhThu(body);
        if (tongMoi < 0) return;

        if (doanhThuCu < 0) {
            doanhThuCu = tongMoi;
            System.out.printf("[%s] [Client-1] Khởi tạo tổng doanh thu: %,.0f đ%n", now(), doanhThuCu);
        } else if (Double.compare(tongMoi, doanhThuCu) != 0) {
            double chenhLech = tongMoi - doanhThuCu;
            System.out.printf("[%s] [Client-1] NHẬN BÁO CÁO MỚI: Tổng doanh thu %,.0f đ -> %,.0f đ (%+,.0f đ)%n",
                    now(), doanhThuCu, tongMoi, chenhLech);
            doanhThuCu = tongMoi;
        }
    }

    // CLIENT-2: Giám sát hóa đơn CHƯA thanh toán -> tự động thông báo sang Client-1 & Client-3
    private static void runClient2ChuaThanhToan() {
        String body = fetchApi("Chưa thanh toán");
        if (body == null) return;

        Map<String, String> danhSachMoi = parseDanhSachHoaDon(body);

        if (chuaThanhToanCu.isEmpty() && !danhSachMoi.isEmpty()) {
            chuaThanhToanCu.putAll(danhSachMoi);
            System.out.printf("[%s] [Client-2] Đang theo dõi %d hóa đơn CHƯA thanh toán.%n",
                    now(), danhSachMoi.size());
            return;
        }

        for (Map.Entry<String, String> item : chuaThanhToanCu.entrySet()) {
            if (!danhSachMoi.containsKey(item.getKey())) {
                String info = item.getValue();
                System.out.printf("[%s] [Client-2] %s vừa ĐƯỢC THANH TOÁN trên Web!%n", now(), info);
                System.out.printf("[%s] [Client-2 -> Client-1] Gửi thông báo: %s đã thanh toán, cập nhật doanh thu.%n",
                        now(), info);
                System.out.printf("[%s] [Client-2 -> Client-3] Gửi thông báo: %s đã thanh toán, tiến hành chốt sổ.%n",
                        now(), info);
            }
        }

        chuaThanhToanCu.clear();
        chuaThanhToanCu.putAll(danhSachMoi);
    }

    // CLIENT-3: Giám sát hóa đơn ĐÃ thanh toán -> tự động ghi nhận chốt sổ
    private static void runClient3DaThanhToan() {
        String body = fetchApi("Đã thanh toán");
        if (body == null) return;

        Map<String, String> danhSachMoi = parseDanhSachHoaDon(body);

        if (daThanhToanCu.isEmpty() && !danhSachMoi.isEmpty()) {
            daThanhToanCu.putAll(danhSachMoi);
            System.out.printf("[%s] [Client-3] Đang quản lý %d hóa đơn ĐÃ thanh toán.%n",
                    now(), danhSachMoi.size());
            return;
        }

        for (Map.Entry<String, String> item : danhSachMoi.entrySet()) {
            if (!daThanhToanCu.containsKey(item.getKey())) {
                System.out.printf("[%s] [Client-3] Đã nhận thông báo & ghi nhận CHỐT SỔ: %s (Tổng đã thu: %d HĐ)%n",
                        now(), item.getValue(), danhSachMoi.size());
            }
        }

        daThanhToanCu.clear();
        daThanhToanCu.putAll(danhSachMoi);
    }

    private static double parseTongDoanhThu(String body) {
        double tong = 0;
        boolean coRow = false;
        for (String line : body.split("\\R")) {
            String[] p = line.split("\\|");
            if ("SUMMARY".equals(p[0]) && p.length >= 2) {
                try {
                    return Double.parseDouble(p[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if ("ROW".equals(p[0]) && p.length >= 8) {
                coRow = true;
                try {
                    tong += Double.parseDouble(p[7].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return coRow ? tong : -1;
    }

    private static Map<String, String> parseDanhSachHoaDon(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : body.split("\\R")) {
            String[] p = line.split("\\|");
            if ("ROW".equals(p[0]) && p.length >= 4) {
                String maHd = p[1].trim();
                String phong = p.length >= 3 ? p[2].trim() : "";
                String khach = p[3].trim();
                String tienStr = "";
                if (p.length >= 8) {
                    try {
                        tienStr = String.format(" - %,.0f đ", Double.parseDouble(p[7].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                map.put(maHd, String.format("HĐ %s (Phòng %s - KH: %s%s)", maHd, phong, khach, tienStr));
            }
        }
        return map;
    }

    private static String fetchApi(String trangThai) {
        try {
            String urlStr = BASE_URL;
            if (trangThai != null && !trangThai.isBlank()) {
                urlStr += "?trangThai=" + URLEncoder.encode(trangThai, StandardCharsets.UTF_8);
            }
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[" + now() + "] Lỗi gọi API: " + e.getMessage());
            return null;
        }
    }

    private static String now() {
        return LocalTime.now().format(FMT);
    }
}