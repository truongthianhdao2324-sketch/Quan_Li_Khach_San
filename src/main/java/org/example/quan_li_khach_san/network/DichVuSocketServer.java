package org.example.quan_li_khach_san.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/** Máy chủ thông báo nội bộ: mỗi kết nối có một luồng xử lý. */
public class DichVuSocketServer implements AutoCloseable {
    private final ServerSocket mayChu;
    private final List<XuLyKetNoi> ketNoi = new ArrayList<>();
    private final List<XuLyKetNoi> mayNhan = new ArrayList<>();
    private volatile boolean dangChay = true;
    private int soThuTu;

    public DichVuSocketServer(String diaChi, int cong) throws IOException {
        mayChu = new ServerSocket();
        mayChu.setReuseAddress(true);
        mayChu.bind(new InetSocketAddress(diaChi, cong));
    }
    public int getPort() { return mayChu.getLocalPort(); }
    public int getSubscriberCount() { synchronized (ketNoi) { return mayNhan.size(); } }

    public void serve() throws IOException {
        System.out.println("Máy chủ dịch vụ đang nghe: " + mayChu.getLocalSocketAddress());
        while (dangChay) {
            try {
                Socket socket = mayChu.accept();
                XuLyKetNoi xuLy = new XuLyKetNoi(socket);
                synchronized (ketNoi) {
                    if (!dangChay || ketNoi.size() >= 32) { socket.close(); continue; }
                    ketNoi.add(xuLy);
                    xuLy.luong = new Thread(xuLy, "dich-vu-" + ++soThuTu);
                    xuLy.luong.start();
                }
            } catch (SocketException loi) {
                if (dangChay) throw loi;
            }
        }
    }

    // Giới hạn độ dài để không đọc mãi một thông điệp sai định dạng.
    public static String docDong(Reader dauVao) throws IOException {
        StringBuilder dong = new StringBuilder();
        int kyTu;
        while ((kyTu = dauVao.read()) != -1) {
            if (kyTu == '\n') return dong.toString();
            if (kyTu == '\r') {
                if (dauVao.read() == '\n') return dong.toString();
                throw new IOException("Dòng dữ liệu sai định dạng.");
            }
            if (dong.length() >= 4096) throw new IOException("Thông điệp quá dài.");
            dong.append((char) kyTu);
        }
        return dong.isEmpty() ? null : dong.toString();
    }

    private class XuLyKetNoi implements Runnable {
        final Socket socket;
        BufferedWriter dauRa;
        Thread luong;
        XuLyKetNoi(Socket socket) { this.socket = socket; }

        synchronized void gui(String dong) throws IOException {
            dauRa.write(dong);
            dauRa.newLine();
            dauRa.flush();
        }

        @Override
        public void run() {
            try (socket;
                 Reader dauVao = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                dauRa = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                socket.setSoTimeout(5000);
                String dong = docDong(dauVao);
                if (dong == null) return;
                String[] phan = dong.split("\\|", -1);
                if (phan.length == 2 && phan[0].equals("NHAN") && !phan[1].isBlank() && phan[1].length() <= 60) {
                    synchronized (ketNoi) {
                        mayNhan.add(this);
                        gui("SAN_SANG");
                    }
                    socket.setSoTimeout(0);
                    System.out.println("[" + Thread.currentThread().getName() + "] Đã kết nối: " + phan[1]);
                    while ((dong = docDong(dauVao)) != null) {
                        if (dong.equals("KIEM_TRA")) gui("DANG_KET_NOI");
                        else { gui("LOI|Lệnh không hợp lệ"); break; }
                    }
                } else if (phan.length == 7 && phan[0].equals("GUI")) {
                    long id = Long.parseLong(phan[1]);
                    long maLuotO = Long.parseLong(phan[2]);
                    int soLuong = Integer.parseInt(phan[5]);
                    LocalDateTime.parse(phan[6]);
                    if (id <= 0 || maLuotO <= 0 || soLuong < 1 || soLuong > 999 || phan[3].isBlank() || phan[4].isBlank())
                        throw new IllegalArgumentException("Thông tin dịch vụ không hợp lệ.");
                    List<XuLyKetNoi> danhSach;
                    synchronized (ketNoi) { danhSach = new ArrayList<>(mayNhan); }
                    int daNhan = 0;
                    String thongBao = "DICH_VU" + dong.substring(3);
                    for (XuLyKetNoi nguoiNhan : danhSach) {
                        try { nguoiNhan.gui(thongBao); daNhan++; }
                        catch (IOException loi) { nguoiNhan.dong(); }
                    }
                    gui("DA_GUI|" + id + "|" + daNhan);
                    System.out.println("[" + Thread.currentThread().getName() + "] Khoản #" + id + " gửi tới " + daNhan + " máy nhận.");
                } else {
                    gui("LOI|Lệnh không hợp lệ");
                }
            } catch (IOException | IllegalArgumentException | java.time.DateTimeException loi) {
                if (dangChay) System.err.println("[" + Thread.currentThread().getName() + "] Ngắt kết nối: " + loi.getMessage());
            } finally {
                synchronized (ketNoi) { ketNoi.remove(this); mayNhan.remove(this); }
                dong();
            }
        }
        void dong() { try { socket.close(); } catch (IOException boQua) { } }
    }

    @Override
    public void close() throws IOException {
        dangChay = false;
        mayChu.close();
        List<XuLyKetNoi> danhSach;
        synchronized (ketNoi) { danhSach = new ArrayList<>(ketNoi); }
        for (XuLyKetNoi xuLy : danhSach) xuLy.dong();
        for (XuLyKetNoi xuLy : danhSach) {
            try { xuLy.luong.join(1000); }
            catch (InterruptedException loi) { Thread.currentThread().interrupt(); break; }
        }
    }

    public static void main(String[] args) {
        int cong = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        String diaChi = args.length > 1 ? args[1] : "127.0.0.1";
        try (DichVuSocketServer mayChu = new DichVuSocketServer(diaChi, cong)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { mayChu.close(); } catch (IOException boQua) { }
            }));
            mayChu.serve();
        } catch (IOException | IllegalArgumentException loi) {
            System.err.println("Không mở được máy chủ dịch vụ: " + loi.getMessage());
        }
    }
}
