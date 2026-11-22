package org.example.quan_li_khach_san;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeToanClientApp {

    private static final String BASE_URL = "http://localhost:8080/api/hoadon/dong-bo";

    public static void main(String[] args) {
        String[][] danhSachClient = {
                {"Client-KeToan-1", ""},
                {"Client-KeToan-2", "Đã thanh toán"},
                {"Client-KeToan-3", "Chưa thanh toán"}
        };

        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (String[] cfg : danhSachClient) {
            pool.submit(() -> ketNoiDongBo(cfg[0], cfg[1]));
        }
        pool.shutdown();
    }

    private static void ketNoiDongBo(String tenClient, String trangThai) {
        try {
            String fullUrl = BASE_URL;
            if (!trangThai.isEmpty()) {
                fullUrl += "?trangThai=" + URLEncoder.encode(trangThai, StandardCharsets.UTF_8);
            }

            URL url = new URL(fullUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                System.out.println("=== [" + tenClient + "] Kết nối: " + fullUrl + " ===");
                while ((line = br.readLine()) != null) {
                    System.out.println("[" + tenClient + "] " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("[" + tenClient + "] Lỗi kết nối: " + e.getMessage());
        }
    }
}