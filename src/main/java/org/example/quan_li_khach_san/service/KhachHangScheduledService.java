package org.example.quan_li_khach_san.service;

import org.example.quan_li_khach_san.entity.KhachHang;
import org.example.quan_li_khach_san.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KhachHangScheduledService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    private static final String LOG_FILE_PATH = "logs/khach_hang_scheduled.log";
    private String lastExecutionSummary = "Chưa có lần quét định kỳ nào được ghi nhận.";

    @Scheduled(fixedRate = 60000)
    public synchronized void tuDongQuetVaGhiLogKhachHang() {
        String threadName = Thread.currentThread().getName();
        String timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<KhachHang> danhSachKH = khachHangRepository.findAll();

        long tongKhach = danhSachKH.size();
        long dangHoatDong = danhSachKH.stream()
                .filter(kh -> !"Đã hủy".equalsIgnoreCase(kh.getTrangThai()))
                .count();
        long daHuy = danhSachKH.stream()
                .filter(kh -> "Đã hủy".equalsIgnoreCase(kh.getTrangThai()))
                .count();

        String logMessage = String.format(
                "[%s] [Thread: %s] Quét định kỳ hồ sơ khách hàng -> Tổng: %d | Hoạt động: %d | Đã hủy: %d",
                timeNow, threadName, tongKhach, dangHoatDong, daHuy
        );

        this.lastExecutionSummary = logMessage;
        System.out.println(logMessage);
        ghiLogRaFile(logMessage);
    }

    private void ghiLogRaFile(String noiDung) {
        try {
            File logFile = new File(LOG_FILE_PATH);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(noiDung);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[LỖI GHI LOG FILE - TV1]: " + e.getMessage());
        }
    }

    public String getLastExecutionSummary() {
        return lastExecutionSummary;
    }

    public List<String> docLichSuLogTuFile() {
        List<String> lines = new ArrayList<>();
        File logFile = new File(LOG_FILE_PATH);
        if (!logFile.exists()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            lines.add("Lỗi đọc file log: " + e.getMessage());
        }
        Collections.reverse(lines);
        return lines.size() > 10 ? lines.subList(0, 10) : lines;
    }
}