package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.service.RoomLogService;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.concurrent.*;

@Service
public class RoomLogService {

    private static final Path LOG_FILE =
            Paths.get("logs", "room-activity.log");

    // Ghi lịch sử thao tác vào file
    public synchronized void ghiLog(String soPhong, String thaoTac,
                                    String trangThaiCu, String trangThaiMoi,
                                    String ketQua) {

        try {
            Files.createDirectories(LOG_FILE.getParent());

            String dongLog = String.format(
                    "%s | Luồng: %s | Phòng: %s | %s | %s -> %s | %s",
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    Thread.currentThread().getName(),
                    soPhong,
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
            System.err.println("Không thể ghi file log: " + e.getMessage());
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
            System.err.println("Không thể đọc file log: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}

