package org.example.quan_li_khach_san.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KhachHangScheduledService {

    private static final Path LOG_FILE =
            Paths.get("logs", "khach-hang-activity.log");

    // Ghi lịch sử thao tác khách hàng vào file (đồng bộ luồng giống RoomLogService)
    public synchronized void ghiLog(String maKH, String hoTen, String thaoTac,
                                    String trangThaiCu, String trangThaiMoi,
                                    String ketQua) {
        try {
            Files.createDirectories(LOG_FILE.getParent());

            String tenHienThi = (hoTen == null || hoTen.isBlank()) ? "Chưa xác định" : hoTen.trim();

            String dongLog = String.format(
                    "%s | Luồng: %s | Khách hàng: %s (%s) | %s | %s -> %s | %s",
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    Thread.currentThread().getName(),
                    maKH,
                    tenHienThi,
                    thaoTac,
                    trangThaiCu,
                    trangThaiMoi,
                    ketQua
            );

            Files.writeString(
                    LOG_FILE,
                    dongLog + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println(dongLog);

        } catch (IOException e) {
            System.err.println("Không thể ghi file log khách hàng: " + e.getMessage());
        }
    }

    // Đọc 50 dòng log mới nhất
    public List<String> docLog() {
        try {
            if (!Files.exists(LOG_FILE)) {
                return Collections.emptyList();
            }

            List<String> logs = Files.readAllLines(
                    LOG_FILE, StandardCharsets.UTF_8);

            Collections.reverse(logs);

            return logs.stream()
                    .limit(50)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("Không thể đọc file log khách hàng: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}