package org.example.quan_li_khach_san.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/** Chạy riêng ở quầy phục vụ; không cần kết nối database. */
public class DichVuSocketClient {
    public static void main(String[] args) {
        String diaChi = args.length > 0 ? args[0] : "127.0.0.1";
        int cong = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String ten = args.length > 2 ? args[2] : "Phuc-vu-1";
        if (ten.isBlank() || ten.length() > 60 || ten.contains("|") || ten.contains("\n") || ten.contains("\r")) {
            System.err.println("Tên máy nhận không hợp lệ."); return;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(diaChi, cong), 3000);
            socket.setSoTimeout(5000);
            Writer dauRa = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            Reader dauVao = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            dauRa.write("NHAN|" + ten + "\n"); dauRa.flush();
            if (!"SAN_SANG".equals(DichVuSocketServer.docDong(dauVao))) throw new IOException("Máy chủ từ chối kết nối.");
            socket.setSoTimeout(0);
            System.out.println("[" + ten + "] Đã kết nối, đang chờ yêu cầu dịch vụ.");
            Set<String> daNhan = new LinkedHashSet<>();
            String dong;
            while ((dong = DichVuSocketServer.docDong(dauVao)) != null) {
                String[] phan = dong.split("\\|", -1);
                if (phan.length != 7 || !phan[0].equals("DICH_VU") || !daNhan.add(phan[1])) continue;
                if (daNhan.size() > 1000) daNhan.remove(daNhan.iterator().next());
                System.out.printf("#%s | Lượt %s | Phòng %s | %s × %s | %s%n", phan[1], phan[2], phan[3], phan[4], phan[5], phan[6]);
            }
            System.out.println("Máy chủ đã ngắt kết nối.");
        } catch (IOException | IllegalArgumentException loi) {
            System.err.println("Không nhận được thông báo: " + loi.getMessage());
        }
    }
}
